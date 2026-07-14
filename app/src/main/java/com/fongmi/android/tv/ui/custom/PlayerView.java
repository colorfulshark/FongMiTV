package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;

import com.fongmi.android.tv.player.danmaku.DanmakuConfig;
import com.fongmi.android.tv.setting.PlayerSetting;

import okhttp3.OkHttpClient;

/** Official Media3 PlayerView with app-level optional extension hooks. */
public class PlayerView extends androidx.media3.ui.PlayerView {

    private boolean debugViewVisible;
    private View videoSurface;
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
