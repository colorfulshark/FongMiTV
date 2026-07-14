package com.fongmi.android.tv.player.exo;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.preload.PreCacheHelper;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.android.tv.setting.PreloadSetting;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PreCache {

    private static final AtomicInteger THREAD_ID = new AtomicInteger();

    private ThreadPoolExecutor downloadExecutor;
    private PreCacheHelper helper;
    private MediaItem mediaItem;

    public void start(ExoPlayer player, MediaItem mediaItem) {
        this.mediaItem = mediaItem;
        restart();
    }

    public void stop() {
        stopManager();
        mediaItem = null;
    }

    public void release() {
        stop();
        ThreadPoolExecutor executor = downloadExecutor;
        downloadExecutor = null;
        if (executor != null) new Handler(Looper.getMainLooper()).post(executor::shutdownNow);
    }

    private void restart() {
        stopManager();
        if (mediaItem == null) return;
        if (!PreloadSetting.isPreload()) return;
        if (!canPreload(mediaItem)) return;
        helper = createHelper(mediaItem);
        helper.preCache(0, PreloadSetting.getPreloadDurationMs());
    }

    private void stopManager() {
        if (helper == null) return;
        helper.release(false);
        helper = null;
    }

    private PreCacheHelper createHelper(MediaItem mediaItem) {
        return new PreCacheHelper.Factory(MediaSourceFactory.getCache(), MediaSourceFactory.createUpstreamDataSourceFactory(ExoUtil.extractHeaders(mediaItem)), ExoUtil.buildRenderersFactory(), Looper.getMainLooper()).setDownloadExecutor(getDownloadExecutor()).create(mediaItem);
    }

    private ThreadPoolExecutor getDownloadExecutor() {
        int threads = PreloadSetting.getPreloadThreads();
        if (downloadExecutor == null || downloadExecutor.isShutdown()) {
            downloadExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), task -> new Thread(task, "PreCache-Download-" + THREAD_ID.incrementAndGet()));
        } else if (downloadExecutor.getCorePoolSize() != threads) {
            resizeDownloadExecutor(threads);
        }
        return downloadExecutor;
    }

    private void resizeDownloadExecutor(int threads) {
        if (threads > downloadExecutor.getMaximumPoolSize()) {
            downloadExecutor.setMaximumPoolSize(threads);
            downloadExecutor.setCorePoolSize(threads);
        } else {
            downloadExecutor.setCorePoolSize(threads);
            downloadExecutor.setMaximumPoolSize(threads);
        }
    }

    private boolean canPreload(MediaItem mediaItem) {
        if (mediaItem.localConfiguration == null) return false;
        String scheme = mediaItem.localConfiguration.uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}
