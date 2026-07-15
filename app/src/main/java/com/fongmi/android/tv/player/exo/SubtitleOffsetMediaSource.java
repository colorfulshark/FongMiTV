package com.fongmi.android.tv.player.exo;

import static java.util.Objects.requireNonNull;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.StreamKey;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.util.NullableType;
import androidx.media3.datasource.TransferListener;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.LoadingInfo;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.ClippingMediaPeriod;
import androidx.media3.exoplayer.source.CompositeMediaSource;
import androidx.media3.exoplayer.source.MediaPeriod;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.SampleStream;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.ExoTrackSelection;
import androidx.media3.exoplayer.trackselection.ForwardingTrackSelection;
import androidx.media3.exoplayer.upstream.Allocator;

import java.io.IOException;
import java.util.List;

/** Adds subtitle offset and uses a bounded secondary source only for positive-offset preroll. */
final class SubtitleOffsetMediaSource extends CompositeMediaSource<Integer> {

    private static final int SOURCE_MAIN = 0;
    private static final int SOURCE_PREROLL = 1;

    private final MediaSource mainSource;
    @Nullable private final MediaSource prerollSource;
    private final long offsetUs;
    @Nullable private Timeline mainTimeline;
    @Nullable private Timeline prerollTimeline;

    public SubtitleOffsetMediaSource(MediaSource mainSource, @Nullable MediaSource prerollSource, long offsetUs) {
        this.mainSource = mainSource;
        this.prerollSource = prerollSource;
        this.offsetUs = offsetUs;
    }

    @Override
    public MediaItem getMediaItem() {
        return mainSource.getMediaItem();
    }

    @Override
    public boolean canUpdateMediaItem(MediaItem mediaItem) {
        return mainSource.canUpdateMediaItem(mediaItem) && (prerollSource == null || prerollSource.canUpdateMediaItem(mediaItem));
    }

    @Override
    public void updateMediaItem(MediaItem mediaItem) {
        mainSource.updateMediaItem(mediaItem);
        if (prerollSource != null) prerollSource.updateMediaItem(mediaItem);
    }

    @Nullable
    @Override
    public Timeline getInitialTimeline() {
        return mainSource.getInitialTimeline();
    }

    @Override
    public boolean isSingleWindow() {
        return mainSource.isSingleWindow();
    }

    @Override
    protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
        super.prepareSourceInternal(mediaTransferListener);
        prepareChildSource(SOURCE_MAIN, mainSource);
        if (prerollSource != null) prepareChildSource(SOURCE_PREROLL, prerollSource);
    }

    @Override
    protected void releaseSourceInternal() {
        super.releaseSourceInternal();
        mainTimeline = null;
        prerollTimeline = null;
    }

    @Override
    protected void onChildSourceInfoRefreshed(Integer childSourceId, MediaSource mediaSource, Timeline timeline) {
        if (childSourceId == SOURCE_MAIN) mainTimeline = timeline;
        else prerollTimeline = timeline;
        if (mainTimeline != null && (prerollSource == null || prerollTimeline != null)) refreshSourceInfo(mainTimeline);
    }

    @Nullable
    @Override
    protected MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(Integer childSourceId, MediaPeriodId mediaPeriodId) {
        if (childSourceId == SOURCE_MAIN || mainTimeline == null || prerollTimeline == null) return mediaPeriodId;
        int periodIndex = prerollTimeline.getIndexOfPeriod(mediaPeriodId.periodUid);
        return periodIndex == C.INDEX_UNSET ? null : mediaPeriodId.copyWithPeriodUid(mainTimeline.getUidOfPeriod(periodIndex));
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
        Timeline mainTimeline = requireNonNull(this.mainTimeline);
        int periodIndex = mainTimeline.getIndexOfPeriod(id.periodUid);
        MediaPeriodId mainId = id.copyWithPeriodUid(mainTimeline.getUidOfPeriod(periodIndex));
        MediaPeriod mainPeriod = mainSource.createPeriod(mainId, allocator, startPositionUs);
        if (prerollSource == null) return new OffsetMediaPeriod(mainPeriod, null, offsetUs);
        Timeline prerollTimeline = requireNonNull(this.prerollTimeline);
        MediaPeriodId prerollId = id.copyWithPeriodUid(prerollTimeline.getUidOfPeriod(periodIndex));
        long prerollStartUs = getPrerollStartUs(startPositionUs);
        MediaPeriod childPrerollPeriod = prerollSource.createPeriod(prerollId, allocator, prerollStartUs);
        ClippingMediaPeriod clippedPrerollPeriod = new ClippingMediaPeriod(childPrerollPeriod, false, prerollStartUs, startPositionUs);
        return new OffsetMediaPeriod(mainPeriod, clippedPrerollPeriod, offsetUs);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        OffsetMediaPeriod offsetPeriod = (OffsetMediaPeriod) mediaPeriod;
        mainSource.releasePeriod(offsetPeriod.mainPeriod);
        if (prerollSource != null && offsetPeriod.prerollPeriod != null) prerollSource.releasePeriod(offsetPeriod.prerollPeriod.mediaPeriod);
    }

    private long getPrerollStartUs(long positionUs) {
        return Math.max(0, positionUs - offsetUs);
    }

    private static final class OffsetMediaPeriod implements MediaPeriod, MediaPeriod.Callback {

        private final MediaPeriod mainPeriod;
        @Nullable private final ClippingMediaPeriod prerollPeriod;
        private final long offsetUs;
        @Nullable private Callback callback;
        private int pendingPreparationCount;
        private TrackGroupArray mainTrackGroups;
        @Nullable private TrackGroupArray prerollTrackGroups;
        private CombinedSampleStream[] sampleStreams;

        private OffsetMediaPeriod(MediaPeriod mainPeriod, @Nullable ClippingMediaPeriod prerollPeriod, long offsetUs) {
            this.mainPeriod = mainPeriod;
            this.prerollPeriod = prerollPeriod;
            this.offsetUs = offsetUs;
            this.mainTrackGroups = TrackGroupArray.EMPTY;
            this.sampleStreams = new CombinedSampleStream[0];
        }

        private long getPrerollStartUs(long positionUs) {
            return Math.max(0, positionUs - offsetUs);
        }

        private void updatePrerollWindow(long positionUs) {
            if (prerollPeriod != null) prerollPeriod.updateClipping(getPrerollStartUs(positionUs), positionUs);
        }

        @Override
        public void prepare(Callback callback, long positionUs) {
            this.callback = callback;
            pendingPreparationCount = prerollPeriod == null ? 1 : 2;
            mainPeriod.prepare(this, positionUs);
            if (prerollPeriod != null) {
                updatePrerollWindow(positionUs);
                prerollPeriod.prepare(this, getPrerollStartUs(positionUs));
            }
        }

        @Override
        public void maybeThrowPrepareError() throws IOException {
            mainPeriod.maybeThrowPrepareError();
            if (prerollPeriod != null) prerollPeriod.maybeThrowPrepareError();
        }

        @Override
        public TrackGroupArray getTrackGroups() {
            return mainTrackGroups;
        }

        @Override
        public List<StreamKey> getStreamKeys(List<ExoTrackSelection> trackSelections) {
            return mainPeriod.getStreamKeys(trackSelections);
        }

        @Override
        public long selectTracks(@NullableType ExoTrackSelection[] selections, boolean[] mayRetainStreamFlags, @NullableType SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
            sampleStreams = new CombinedSampleStream[streams.length];
            @NullableType SampleStream[] mainStreams = new SampleStream[streams.length];
            @NullableType SampleStream[] prerollStreams = new SampleStream[streams.length];
            @NullableType ExoTrackSelection[] prerollSelections = new ExoTrackSelection[selections.length];
            boolean[] prerollResetFlags = new boolean[streams.length];
            for (int i = 0; i < streams.length; i++) {
                sampleStreams[i] = streams[i] instanceof CombinedSampleStream stream ? stream : null;
                mainStreams[i] = sampleStreams[i] == null ? streams[i] : sampleStreams[i].mainStream;
                prerollStreams[i] = sampleStreams[i] == null ? null : sampleStreams[i].prerollStream;
                prerollSelections[i] = getPrerollSelection(selections[i]);
            }
            long mainPositionUs = mainPeriod.selectTracks(selections, mayRetainStreamFlags, mainStreams, streamResetFlags, positionUs);
            if (prerollPeriod != null) {
                updatePrerollWindow(mainPositionUs);
                prerollPeriod.selectTracks(prerollSelections, mayRetainStreamFlags, prerollStreams, prerollResetFlags, getPrerollStartUs(mainPositionUs));
            }
            for (int i = 0; i < streams.length; i++) {
                SampleStream mainStream = mainStreams[i];
                SampleStream prerollStream = prerollStreams[i];
                if (mainStream == null) {
                    sampleStreams[i] = null;
                    streams[i] = null;
                } else if (isTextSelection(selections[i])) {
                    CombinedSampleStream oldStream = sampleStreams[i];
                    if (oldStream == null || oldStream.mainStream != mainStream || oldStream.prerollStream != prerollStream) oldStream = new CombinedSampleStream(prerollStream, mainStream, offsetUs, mainPositionUs);
                    else oldStream.resetPreroll(mainPositionUs);
                    sampleStreams[i] = oldStream;
                    streams[i] = oldStream;
                    streamResetFlags[i] |= prerollResetFlags[i];
                } else {
                    sampleStreams[i] = null;
                    streams[i] = mainStream;
                }
            }
            return mainPositionUs;
        }

        @Nullable
        private ExoTrackSelection getPrerollSelection(@Nullable ExoTrackSelection selection) {
            if (prerollPeriod == null || !isTextSelection(selection) || prerollTrackGroups == null) return null;
            int groupIndex = mainTrackGroups.indexOf(selection.getTrackGroup());
            if (groupIndex == C.INDEX_UNSET || groupIndex >= prerollTrackGroups.length) return null;
            TrackGroup prerollGroup = prerollTrackGroups.get(groupIndex);
            return prerollGroup.type == C.TRACK_TYPE_TEXT ? new RemappedTrackSelection(selection, prerollGroup) : null;
        }

        private static boolean isTextSelection(@Nullable ExoTrackSelection selection) {
            return selection != null && selection.getTrackGroup().type == C.TRACK_TYPE_TEXT;
        }

        @Override
        public void discardBuffer(long positionUs, boolean toKeyframe) {
            mainPeriod.discardBuffer(positionUs, toKeyframe);
            if (prerollPeriod != null) prerollPeriod.discardBuffer(getPrerollStartUs(positionUs), toKeyframe);
        }

        @Override
        public long readDiscontinuity() {
            return mainPeriod.readDiscontinuity();
        }

        @Override
        public long seekToUs(long positionUs) {
            long mainPositionUs = mainPeriod.seekToUs(positionUs);
            if (prerollPeriod != null) {
                updatePrerollWindow(mainPositionUs);
                prerollPeriod.seekToUs(getPrerollStartUs(mainPositionUs));
                for (CombinedSampleStream stream : sampleStreams) if (stream != null) stream.resetPreroll(mainPositionUs);
            }
            return mainPositionUs;
        }

        @Override
        public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
            return mainPeriod.getAdjustedSeekPositionUs(positionUs, seekParameters);
        }

        @Override
        public long getBufferedPositionUs() {
            long mainBufferedUs = mainPeriod.getBufferedPositionUs();
            if (prerollPeriod == null) return mainBufferedUs;
            long prerollBufferedUs = prerollPeriod.getBufferedPositionUs();
            if (prerollBufferedUs == C.TIME_END_OF_SOURCE) return mainBufferedUs;
            long offsetPrerollBufferedUs = prerollBufferedUs + offsetUs;
            return mainBufferedUs == C.TIME_END_OF_SOURCE ? offsetPrerollBufferedUs : Math.min(mainBufferedUs, offsetPrerollBufferedUs);
        }

        @Override
        public long getNextLoadPositionUs() {
            long mainLoadUs = mainPeriod.getNextLoadPositionUs();
            if (prerollPeriod == null) return mainLoadUs;
            long prerollLoadUs = prerollPeriod.getNextLoadPositionUs();
            if (prerollLoadUs == C.TIME_END_OF_SOURCE) return mainLoadUs;
            long offsetPrerollLoadUs = prerollLoadUs + offsetUs;
            return mainLoadUs == C.TIME_END_OF_SOURCE ? offsetPrerollLoadUs : Math.min(mainLoadUs, offsetPrerollLoadUs);
        }

        @Override
        public boolean continueLoading(LoadingInfo loadingInfo) {
            boolean loading = mainPeriod.continueLoading(loadingInfo);
            if (prerollPeriod != null && prerollPeriod.getBufferedPositionUs() != C.TIME_END_OF_SOURCE) {
                LoadingInfo prerollInfo = loadingInfo.buildUpon().setPlaybackPositionUs(getPrerollStartUs(loadingInfo.playbackPositionUs)).build();
                loading |= prerollPeriod.continueLoading(prerollInfo);
            }
            return loading;
        }

        @Override
        public boolean isLoading() {
            return mainPeriod.isLoading() || (prerollPeriod != null && prerollPeriod.isLoading());
        }

        @Override
        public void reevaluateBuffer(long positionUs) {
            mainPeriod.reevaluateBuffer(positionUs);
            if (prerollPeriod != null) prerollPeriod.reevaluateBuffer(getPrerollStartUs(positionUs));
        }

        @Override
        public long setEndPositionUs(long endPositionUs) {
            return mainPeriod.setEndPositionUs(endPositionUs);
        }

        @Override
        public void onPrepared(MediaPeriod mediaPeriod) {
            if (mediaPeriod == mainPeriod) mainTrackGroups = mainPeriod.getTrackGroups();
            else if (prerollPeriod != null && mediaPeriod == prerollPeriod) prerollTrackGroups = prerollPeriod.getTrackGroups();
            if (--pendingPreparationCount == 0) requireNonNull(callback).onPrepared(this);
        }

        @Override
        public void onContinueLoadingRequested(MediaPeriod source) {
            requireNonNull(callback).onContinueLoadingRequested(this);
        }
    }

    private static final class CombinedSampleStream implements SampleStream {

        @Nullable private final SampleStream prerollStream;
        private final SampleStream mainStream;
        private final long offsetUs;
        private boolean readingPreroll;
        private boolean mainStreamAligned;
        private long prerollEndUs;

        private CombinedSampleStream(@Nullable SampleStream prerollStream, SampleStream mainStream, long offsetUs, long prerollEndUs) {
            this.prerollStream = prerollStream;
            this.mainStream = mainStream;
            this.offsetUs = offsetUs;
            resetPreroll(prerollEndUs);
        }

        private void resetPreroll(long prerollEndUs) {
            readingPreroll = prerollStream != null;
            mainStreamAligned = prerollStream == null;
            this.prerollEndUs = prerollEndUs;
        }

        @Override
        public boolean isReady() {
            return readingPreroll ? requireNonNull(prerollStream).isReady() : mainStream.isReady();
        }

        @Override
        public void maybeThrowError() throws IOException {
            if (readingPreroll) requireNonNull(prerollStream).maybeThrowError();
            mainStream.maybeThrowError();
        }

        @Override
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            int result = readingPreroll ? requireNonNull(prerollStream).readData(formatHolder, buffer, readFlags) : mainStreamAligned ? mainStream.readData(formatHolder, buffer, readFlags) : readMainData(formatHolder, buffer, readFlags);
            if (result == C.RESULT_BUFFER_READ && buffer.isEndOfStream() && readingPreroll) {
                readingPreroll = false;
                buffer.clear();
                mainStream.skipData(prerollEndUs);
                result = readMainData(formatHolder, buffer, readFlags);
            }
            if (result == C.RESULT_BUFFER_READ && !buffer.isEndOfStream()) buffer.timeUs += offsetUs;
            return result;
        }

        private int readMainData(FormatHolder formatHolder, DecoderInputBuffer buffer, int readFlags) {
            while (true) {
                int result = mainStream.readData(formatHolder, buffer, readFlags);
                if (result != C.RESULT_BUFFER_READ || buffer.isEndOfStream() || buffer.timeUs >= prerollEndUs) {
                    if (result == C.RESULT_BUFFER_READ) mainStreamAligned = true;
                    return result;
                }
                if ((readFlags & SampleStream.FLAG_PEEK) != 0) {
                    buffer.clear();
                    int consumeFlags = (readFlags & ~(SampleStream.FLAG_PEEK | SampleStream.FLAG_REQUIRE_FORMAT)) | SampleStream.FLAG_OMIT_SAMPLE_DATA;
                    mainStream.readData(formatHolder, buffer, consumeFlags);
                }
                buffer.clear();
            }
        }

        @Override
        public int skipData(long positionUs) {
            int skipped = 0;
            long childPositionUs = positionUs - offsetUs;
            if (readingPreroll) skipped += requireNonNull(prerollStream).skipData(childPositionUs);
            skipped += mainStream.skipData(childPositionUs);
            return skipped;
        }
    }

    private static final class RemappedTrackSelection extends ForwardingTrackSelection {

        private final TrackGroup trackGroup;

        private RemappedTrackSelection(ExoTrackSelection selection, TrackGroup trackGroup) {
            super(selection);
            this.trackGroup = trackGroup;
        }

        @Override
        public TrackGroup getTrackGroup() {
            return trackGroup;
        }

        @Override
        public Format getFormat(int index) {
            return trackGroup.getFormat(getWrappedInstance().getIndexInTrackGroup(index));
        }

        @Override
        public int indexOf(Format format) {
            return getWrappedInstance().indexOf(trackGroup.indexOf(format));
        }

        @Override
        public Format getSelectedFormat() {
            return trackGroup.getFormat(getWrappedInstance().getSelectedIndexInTrackGroup());
        }
    }
}
