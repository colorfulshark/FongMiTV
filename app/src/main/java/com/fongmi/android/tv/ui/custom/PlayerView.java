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
import com.fongmi.android.tv.player.danmaku.DanmakuConfig;
import com.fongmi.android.tv.setting.PlayerSetting;

import okhttp3.OkHttpClient;

/** Official Media3 PlayerView with app-level optional extension hooks. */
public class PlayerView extends androidx.media3.ui.PlayerView {

    private static final long FRAME_RATE_UPDATE_INTERVAL_MS = 1000L;

    private final Runnable frameRateUpdater = this::updateFrameRate;
    private boolean debugViewVisible;
    private TextView frameRateView;
    private View videoSurface;
    private int renderedFrameCount;
    private long renderedFrameTimeMs;
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
        View newSurface = target == PlayerSetting.RENDER_TEXTURE ? new TextureView(getContext()) : new SurfaceView(getContext());
        newSurface.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        newSurface.setClickable(false);
        content.addView(newSurface, 0);

        Player player = getPlayer();
        if (player != null && player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) {
            attachSurface(player, newSurface);
            clearSurface(player, oldSurface);
        }
        if (oldSurface != null) content.removeView(oldSurface);
        videoSurface = newSurface;
        this.render = target;
    }

    @Override
    public void setPlayer(@Nullable Player player) {
        Player oldPlayer = getPlayer();
        if (oldPlayer == player) return;
        if (oldPlayer != null && oldPlayer.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) clearSurface(oldPlayer, videoSurface);
        super.setPlayer(player);
        if (player != null && player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) attachSurface(player, videoSurface);
        resetFrameRateSample();
    }

    public void refreshFrameRateOverlay() {
        removeCallbacks(frameRateUpdater);
        boolean visible = PlayerSetting.isFrameRateVisible();
        frameRateView.setVisibility(visible ? VISIBLE : GONE);
        resetFrameRateSample();
        if (visible && isAttachedToWindow()) {
            updateFrameRate();
        }
    }

    private void resetFrameRateSample() {
        renderedFrameCount = getRenderedFrameCount();
        renderedFrameTimeMs = SystemClock.elapsedRealtime();
    }

    private void updateFrameRate() {
        if (!PlayerSetting.isFrameRateVisible() || !isAttachedToWindow()) return;
        long now = SystemClock.elapsedRealtime();
        int currentFrameCount = getRenderedFrameCount();
        long elapsedMs = now - renderedFrameTimeMs;
        float videoFrameRate = elapsedMs > 0 && currentFrameCount >= renderedFrameCount ? (currentFrameCount - renderedFrameCount) * 1000f / elapsedMs : 0f;
        Display display = getDisplay();
        float refreshRate = display == null ? 0f : display.getRefreshRate();
        frameRateView.setText(getResources().getString(R.string.player_frame_rate_info, videoFrameRate, refreshRate));
        renderedFrameCount = currentFrameCount;
        renderedFrameTimeMs = now;
        removeCallbacks(frameRateUpdater);
        postDelayed(frameRateUpdater, FRAME_RATE_UPDATE_INTERVAL_MS);
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
