package com.fongmi.android.tv.player;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.setting.PlayerSetting;

/** Coordinates Media3 frame-rate hints and non-seamless display refresh-rate changes. */
public final class AutoFrameRateManager implements DisplayManager.DisplayListener {

    private static final float RATE_EPSILON = 0.02f;
    private static final float DEFAULT_ALWAYS_FRAME_RATE = 25f;
    private static final long DISPLAY_SWITCH_TIMEOUT_MS = 2500L;
    private static final float[] STANDARD_FRAME_RATES = {23.976f, 24f, 25f, 29.97f, 30f, 47.952f, 48f, 50f, 59.94f, 60f, 100f, 119.88f, 120f};

    private final DisplayManager displayManager;
    private final Handler mainHandler;
    private final View hostView;
    private final Runnable switchTimeout = this::finishDisplaySwitch;
    private final Runnable switchSettled = this::finishDisplaySwitch;

    private int appliedMode = PlayerSetting.AUTO_FRAME_RATE_OFF;
    private int originalPreferredDisplayModeId;
    private boolean originalDisplayModeCaptured;
    private boolean displayListenerRegistered;
    private boolean resumeAfterSwitch;
    private float appliedFrameRate;
    private long playbackIntentVersionAtSwitch;
    private Player switchingPlayer;
    private View appliedSurfaceView;

    public AutoFrameRateManager(@NonNull View hostView) {
        this.hostView = hostView;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.displayManager = (DisplayManager) hostView.getContext().getSystemService(Context.DISPLAY_SERVICE);
    }

    public void configurePlayer(@Nullable Player player) {
        if (!(player instanceof ExoPlayer exoPlayer)) return;
        int strategy = PlayerSetting.getAutoFrameRate() == PlayerSetting.AUTO_FRAME_RATE_SEAMLESS ? C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_ONLY_IF_SEAMLESS : C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF;
        exoPlayer.setVideoChangeFrameRateStrategy(strategy);
    }

    public float getContentFrameRate(@Nullable Player player) {
        if (player == null) return Format.NO_VALUE;
        if (player instanceof ExoPlayer exoPlayer) {
            Format format = exoPlayer.getVideoFormat();
            if (format != null && format.frameRate > 0) return normalizeFrameRate(format.frameRate);
        }
        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int i = 0; i < group.length; i++) {
                Format format = group.getTrackFormat(i);
                if (group.isTrackSelected(i) && format.frameRate > 0) return normalizeFrameRate(format.frameRate);
            }
        }
        return Format.NO_VALUE;
    }

    public void apply(@Nullable Player player, @Nullable View surfaceView, float measuredFrameRate) {
        configurePlayer(player);
        int mode = PlayerSetting.getAutoFrameRate();
        if (player == null || mode == PlayerSetting.AUTO_FRAME_RATE_OFF) {
            detachSurface(surfaceView);
            return;
        }

        float contentFrameRate = getContentFrameRate(player);
        float requestedFrameRate;
        if (contentFrameRate > 0) {
            requestedFrameRate = contentFrameRate * player.getPlaybackParameters().speed;
        } else if (measuredFrameRate > 0) {
            // Measured FPS is sampled against elapsed real time, so playback speed is already reflected.
            requestedFrameRate = measuredFrameRate;
        } else if (mode == PlayerSetting.AUTO_FRAME_RATE_ALWAYS) {
            requestedFrameRate = DEFAULT_ALWAYS_FRAME_RATE * player.getPlaybackParameters().speed;
        } else {
            return;
        }
        requestedFrameRate = normalizeFrameRate(requestedFrameRate);

        if (mode == PlayerSetting.AUTO_FRAME_RATE_SEAMLESS) {
            if (appliedMode == PlayerSetting.AUTO_FRAME_RATE_ALWAYS) clearAppRequest();
            appliedMode = mode;
            appliedFrameRate = requestedFrameRate;
            appliedSurfaceView = surfaceView;
            return;
        }

        if (appliedMode == mode && appliedSurfaceView == surfaceView && Math.abs(appliedFrameRate - requestedFrameRate) < RATE_EPSILON) return;
        boolean requestWillSwitch = !isCurrentRefreshRateCompatible(requestedFrameRate);
        boolean requested;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean canSwitchNonSeamlessly = displayManager != null && displayManager.getMatchContentFrameRateUserPreference() == DisplayManager.MATCH_CONTENT_FRAMERATE_ALWAYS;
            if (requestWillSwitch && canSwitchNonSeamlessly) beginDisplaySwitch(player);
            requested = setSurfaceFrameRate(surfaceView, requestedFrameRate, Surface.CHANGE_FRAME_RATE_ALWAYS);
        } else {
            Display.Mode targetMode = findBestDisplayMode(requestedFrameRate);
            if (requestWillSwitch && targetMode != null) beginDisplaySwitch(player);
            requested = setPreferredDisplayMode(targetMode);
        }
        if (!requested) finishDisplaySwitch();
        appliedMode = requested ? mode : PlayerSetting.AUTO_FRAME_RATE_OFF;
        appliedFrameRate = requested ? requestedFrameRate : 0;
        appliedSurfaceView = requested ? surfaceView : null;
    }

    public void clearSurface(@Nullable View currentSurfaceView) {
        clear(currentSurfaceView, false, true);
    }

    public void detachSurface(@Nullable View currentSurfaceView) {
        clear(currentSurfaceView, true, true);
    }

    public void release(@Nullable View currentSurfaceView) {
        clear(currentSurfaceView, true, false);
    }

    private void clear(@Nullable View currentSurfaceView, boolean restoreDisplayMode, boolean allowResume) {
        if (appliedMode == PlayerSetting.AUTO_FRAME_RATE_ALWAYS) clearSurfaceFrameRate(appliedSurfaceView != null ? appliedSurfaceView : currentSurfaceView);
        if (restoreDisplayMode) restorePreferredDisplayMode();
        if (allowResume) finishDisplaySwitch();
        else cancelDisplaySwitch();
        appliedMode = PlayerSetting.AUTO_FRAME_RATE_OFF;
        appliedFrameRate = 0;
        appliedSurfaceView = null;
    }

    private void clearAppRequest() {
        if (appliedMode == PlayerSetting.AUTO_FRAME_RATE_ALWAYS) clearSurfaceFrameRate(appliedSurfaceView);
        restorePreferredDisplayMode();
    }

    private boolean setSurfaceFrameRate(@Nullable View view, float frameRate, int strategy) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || view == null) return false;
        Surface surface = null;
        boolean releaseSurface = false;
        if (view instanceof SurfaceView surfaceView) {
            surface = surfaceView.getHolder().getSurface();
        } else if (view instanceof TextureView textureView && textureView.isAvailable()) {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture != null) {
                surface = new Surface(texture);
                releaseSurface = true;
            }
        }
        if (surface == null || !surface.isValid()) {
            if (releaseSurface && surface != null) surface.release();
            return false;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) surface.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE, strategy);
            else surface.setFrameRate(frameRate, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        } finally {
            if (releaseSurface) surface.release();
        }
    }

    private void clearSurfaceFrameRate(@Nullable View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || view == null) return;
        setSurfaceFrameRate(view, 0, Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS);
    }

    @Nullable
    private Display.Mode findBestDisplayMode(float frameRate) {
        Display display = hostView.getDisplay();
        if (display == null) return null;
        Display.Mode current = display.getMode();
        if (isCompatible(current.getRefreshRate(), frameRate)) return current;
        Display.Mode best = null;
        float bestScore = Float.MAX_VALUE;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() != current.getPhysicalWidth() || mode.getPhysicalHeight() != current.getPhysicalHeight()) continue;
            if (!isCompatible(mode.getRefreshRate(), frameRate)) continue;
            float score = Math.abs(mode.getRefreshRate() - current.getRefreshRate());
            if (score < bestScore) {
                best = mode;
                bestScore = score;
            }
        }
        return best;
    }

    private boolean setPreferredDisplayMode(@Nullable Display.Mode mode) {
        Activity activity = findActivity(hostView.getContext());
        if (activity == null || mode == null) return false;
        WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
        if (!originalDisplayModeCaptured) {
            originalPreferredDisplayModeId = attributes.preferredDisplayModeId;
            originalDisplayModeCaptured = true;
        }
        if (attributes.preferredDisplayModeId == mode.getModeId()) return true;
        attributes.preferredDisplayModeId = mode.getModeId();
        activity.getWindow().setAttributes(attributes);
        return true;
    }

    private void restorePreferredDisplayMode() {
        if (!originalDisplayModeCaptured) return;
        Activity activity = findActivity(hostView.getContext());
        if (activity != null) {
            WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
            attributes.preferredDisplayModeId = originalPreferredDisplayModeId;
            activity.getWindow().setAttributes(attributes);
        }
        originalDisplayModeCaptured = false;
    }

    private boolean isCurrentRefreshRateCompatible(float frameRate) {
        Display display = hostView.getDisplay();
        return display != null && isCompatible(display.getRefreshRate(), frameRate);
    }

    private static boolean isCompatible(float refreshRate, float frameRate) {
        if (refreshRate <= 0 || frameRate <= 0) return false;
        int multiple = Math.max(1, Math.round(refreshRate / frameRate));
        return Math.abs(refreshRate - frameRate * multiple) <= Math.max(0.12f, frameRate * 0.003f);
    }

    private static float normalizeFrameRate(float frameRate) {
        if (frameRate <= 0) return frameRate;
        for (float standard : STANDARD_FRAME_RATES) if (Math.abs(frameRate - standard) < 0.35f) return standard;
        return frameRate;
    }

    private void beginDisplaySwitch(@NonNull Player player) {
        finishDisplaySwitch();
        switchingPlayer = player;
        playbackIntentVersionAtSwitch = PlaybackIntentTracker.getVersion(player);
        resumeAfterSwitch = player.getPlayWhenReady();
        if (resumeAfterSwitch) player.setPlayWhenReady(false);
        if (displayManager != null) {
            displayManager.registerDisplayListener(this, mainHandler);
            displayListenerRegistered = true;
        }
        mainHandler.postDelayed(switchTimeout, DISPLAY_SWITCH_TIMEOUT_MS);
    }

    private void finishDisplaySwitch() {
        completeDisplaySwitch(true);
    }

    private void cancelDisplaySwitch() {
        completeDisplaySwitch(false);
    }

    private void completeDisplaySwitch(boolean allowResume) {
        mainHandler.removeCallbacks(switchTimeout);
        mainHandler.removeCallbacks(switchSettled);
        if (displayListenerRegistered && displayManager != null) displayManager.unregisterDisplayListener(this);
        displayListenerRegistered = false;
        boolean playbackIntentUnchanged = switchingPlayer != null && PlaybackIntentTracker.getVersion(switchingPlayer) == playbackIntentVersionAtSwitch;
        if (allowResume && switchingPlayer != null && resumeAfterSwitch && playbackIntentUnchanged) switchingPlayer.setPlayWhenReady(true);
        switchingPlayer = null;
        resumeAfterSwitch = false;
        playbackIntentVersionAtSwitch = 0;
    }

    @Override
    public void onDisplayAdded(int displayId) {
    }

    @Override
    public void onDisplayRemoved(int displayId) {
    }

    @Override
    public void onDisplayChanged(int displayId) {
        Display display = hostView.getDisplay();
        if (display != null && display.getDisplayId() == displayId) {
            mainHandler.removeCallbacks(switchSettled);
            mainHandler.postDelayed(switchSettled, 250L);
        }
    }

    @Nullable
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity activity) return activity;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context instanceof Activity ? (Activity) context : null;
    }
}
