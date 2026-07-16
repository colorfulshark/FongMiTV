package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.AutoFrameRateManager;
import com.fongmi.android.tv.player.danmaku.DanmakuConfig;
import com.fongmi.android.tv.setting.PlayerSetting;

import okhttp3.OkHttpClient;

/** Official Media3 PlayerView with app-level optional extension hooks. */
public class PlayerView extends androidx.media3.ui.PlayerView {

    private static final long FRAME_RATE_UPDATE_INTERVAL_MS = 1000L;

    private final Runnable frameRateUpdater = this::updateFrameRate;
    private final Player.Listener frameRateListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            if (player.getPlaybackState() == Player.STATE_ENDED) autoFrameRateManager.release(videoSurface);
            else if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_PLAYBACK_PARAMETERS_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAYBACK_STATE_CHANGED)) post(PlayerView.this::refreshAutoFrameRate);
        }

        @Override
        public void onRenderedFirstFrame() {
            post(PlayerView.this::refreshAutoFrameRate);
        }
    };
    private AutoFrameRateManager autoFrameRateManager;
    private boolean debugViewVisible;
    private boolean frameRatePreMatch;
    private TextView frameRateView;
    private View videoSurface;
    private int renderedFrameCount;
    private long renderedFrameTimeMs;
    private float measuredFrameRateCandidate;
    private float stableMeasuredFrameRate;
    private int measuredFrameRateSamples;
    private int render = -1;

    public PlayerView(@NonNull Context context) {
        super(context);
        initSurface();
    }

    public PlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initSurface();
    }

    public PlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSurface();
    }

    private void initSurface() {
        autoFrameRateManager = new AutoFrameRateManager(this);
        setRender(PlayerSetting.getRender());
        initFrameRateView();
    }

    private void initFrameRateView() {
        frameRateView = new TextView(getContext());
        frameRateView.setTextColor(Color.WHITE);
        frameRateView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        frameRateView.setTypeface(android.graphics.Typeface.MONOSPACE);
        frameRateView.setBackgroundColor(0x99000000);
        int padding = dp(8);
        frameRateView.setPadding(padding, dp(4), padding, dp(4));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        params.setMargins(dp(12), dp(12), dp(12), dp(12));
        addView(frameRateView, params);
        refreshFrameRateOverlay();
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
    }

    public void setRender(int render) {
        int target = render == PlayerSetting.RENDER_TEXTURE ? PlayerSetting.RENDER_TEXTURE : PlayerSetting.RENDER_SURFACE;
        if (this.render == target && videoSurface != null) return;
        FrameLayout content = findViewById(androidx.media3.ui.R.id.exo_content_frame);
        if (content == null) return;

        View oldSurface = videoSurface;
        if (oldSurface != null) autoFrameRateManager.clearSurface(oldSurface);
        View newSurface = target == PlayerSetting.RENDER_TEXTURE ? new TextureView(getContext()) : new SurfaceView(getContext());
        newSurface.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSurface.setClickable(false);
        videoSurface = newSurface;
        this.render = target;
        content.addView(newSurface, 0);
        if (newSurface instanceof SurfaceView surfaceView) {
            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    if (videoSurface == surfaceView) post(PlayerView.this::refreshAutoFrameRate);
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                    if (videoSurface == surfaceView) post(PlayerView.this::refreshAutoFrameRate);
                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    if (videoSurface == surfaceView) autoFrameRateManager.clearSurface(surfaceView);
                }
            });
        }

        Player player = getPlayer();
        if (player != null && player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) {
            attachSurface(player, newSurface);
            clearSurface(player, oldSurface);
        }
        if (oldSurface != null) content.removeView(oldSurface);
    }

    @Override
    public void setPlayer(@Nullable Player player) {
        Player oldPlayer = getPlayer();
        frameRatePreMatch = false;
        if (oldPlayer == player) {
            if (player == null) autoFrameRateManager.detachSurface(videoSurface);
            return;
        }
        if (oldPlayer != null && player != null) autoFrameRateManager.release(videoSurface);
        else if (oldPlayer != null) autoFrameRateManager.detachSurface(videoSurface);
        if (oldPlayer != null) oldPlayer.removeListener(frameRateListener);
        if (oldPlayer != null && oldPlayer.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) clearSurface(oldPlayer, videoSurface);
        super.setPlayer(player);
        if (player != null) {
            player.addListener(frameRateListener);
            autoFrameRateManager.configurePlayer(player);
            if (player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) attachSurface(player, videoSurface);
        }
        resetFrameRateSample();
        refreshAutoFrameRate();
    }

    public void beginAutoFrameRatePreMatch() {
        frameRatePreMatch = true;
        refreshAutoFrameRate();
    }

    public void prepareForExit(@NonNull Runnable completion) {
        frameRatePreMatch = false;
        removeCallbacks(frameRateUpdater);
        Player player = getPlayer();
        if (player != null) {
            player.removeListener(frameRateListener);
            if (player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) clearSurface(player, videoSurface);
            super.setPlayer(null);
        }
        resetFrameRateSample();
        autoFrameRateManager.restoreForExit(videoSurface, completion);
    }

    public boolean hasActiveAutoFrameRateRequest() {
        return autoFrameRateManager.hasActiveDisplayRequest();
    }

    public void refreshFrameRateOverlay() {
        removeCallbacks(frameRateUpdater);
        boolean visible = PlayerSetting.isFrameRateVisible();
        frameRateView.setVisibility(visible ? VISIBLE : GONE);
        resetFrameRateSample();
        refreshAutoFrameRate();
        if ((visible || PlayerSetting.getAutoFrameRate() != PlayerSetting.AUTO_FRAME_RATE_OFF) && isAttachedToWindow()) {
            updateFrameRate();
        }
    }

    private void resetFrameRateSample() {
        renderedFrameCount = getRenderedFrameCount();
        renderedFrameTimeMs = SystemClock.elapsedRealtime();
        measuredFrameRateCandidate = 0;
        stableMeasuredFrameRate = 0;
        measuredFrameRateSamples = 0;
    }

    private void updateFrameRate() {
        boolean visible = PlayerSetting.isFrameRateVisible();
        boolean matching = PlayerSetting.getAutoFrameRate() != PlayerSetting.AUTO_FRAME_RATE_OFF;
        if ((!visible && !matching) || !isAttachedToWindow()) return;
        long now = SystemClock.elapsedRealtime();
        int currentFrameCount = getRenderedFrameCount();
        long elapsedMs = now - renderedFrameTimeMs;
        float videoFrameRate = elapsedMs > 0 && currentFrameCount >= renderedFrameCount ? (currentFrameCount - renderedFrameCount) * 1000f / elapsedMs : 0f;
        updateMeasuredFrameRate(videoFrameRate);
        refreshAutoFrameRate();
        Display display = getDisplay();
        float refreshRate = display == null ? 0f : display.getRefreshRate();
        if (visible) frameRateView.setText(getResources().getString(R.string.player_frame_rate_info, videoFrameRate, refreshRate));
        renderedFrameCount = currentFrameCount;
        renderedFrameTimeMs = now;
        removeCallbacks(frameRateUpdater);
        postDelayed(frameRateUpdater, FRAME_RATE_UPDATE_INTERVAL_MS);
    }

    private void updateMeasuredFrameRate(float frameRate) {
        if (frameRate <= 1) return;
        if (Math.abs(frameRate - measuredFrameRateCandidate) < 0.6f) {
            measuredFrameRateSamples++;
        } else {
            measuredFrameRateCandidate = frameRate;
            measuredFrameRateSamples = 1;
        }
        if (measuredFrameRateSamples >= 3) stableMeasuredFrameRate = measuredFrameRateCandidate;
    }

    public void refreshAutoFrameRate() {
        Player player = getPlayer();
        if (player != null) autoFrameRateManager.apply(player, videoSurface, stableMeasuredFrameRate);
        else if (frameRatePreMatch) autoFrameRateManager.preMatch(videoSurface);
    }

    private int getRenderedFrameCount() {
        Player player = getPlayer();
        if (!(player instanceof ExoPlayer exoPlayer)) return 0;
        DecoderCounters counters = exoPlayer.getVideoDecoderCounters();
        if (counters == null) return 0;
        counters.ensureUpdated();
        return counters.renderedOutputBufferCount;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refreshFrameRateOverlay();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(frameRateUpdater);
        autoFrameRateManager.detachSurface(videoSurface);
        super.onDetachedFromWindow();
    }

    @Override
    @Nullable
    public View getVideoSurfaceView() {
        return videoSurface;
    }

    private void attachSurface(@NonNull Player player, @Nullable View surface) {
        if (surface instanceof TextureView) player.setVideoTextureView((TextureView) surface);
        else if (surface instanceof SurfaceView) player.setVideoSurfaceView((SurfaceView) surface);
    }

    private void clearSurface(@NonNull Player player, @Nullable View surface) {
        if (surface instanceof TextureView) player.clearVideoTextureView((TextureView) surface);
        else if (surface instanceof SurfaceView) player.clearVideoSurfaceView((SurfaceView) surface);
    }

    public boolean isDebugViewVisible() { return debugViewVisible; }
    public void toggleDebugView() { debugViewVisible = !debugViewVisible; }
    public void hideDebugView() { debugViewVisible = false; }
    public void setDanmakuOkHttpClient(@Nullable OkHttpClient client) {}
    public void setDanmakuSource(@Nullable Uri uri) {}
    public void setDanmakuConfig(@NonNull DanmakuConfig config) {}
    public void setDanmakuEnabled(boolean enabled) {}
    public void sendDanmaku(@NonNull String text) {}
}
