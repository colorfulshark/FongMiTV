package com.fongmi.android.tv.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.setting.PlayerSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Applies the user-selected default refresh rate to every app activity window. */
public final class DefaultDisplayModeManager {

    private static final float RATE_EPSILON = 0.02f;

    public static void apply(@NonNull Activity activity) {
        apply(activity, activity.getWindow());
    }

    public static void inherit(@NonNull Context context, @NonNull Dialog dialog) {
        Activity activity = findActivity(context);
        if (activity == null) return;
        Window dialogWindow = dialog.getWindow();
        if (dialogWindow == null) return;
        WindowManager.LayoutParams hostAttributes = activity.getWindow().getAttributes();
        WindowManager.LayoutParams dialogAttributes = dialogWindow.getAttributes();
        if (dialogAttributes.preferredDisplayModeId == hostAttributes.preferredDisplayModeId && Math.abs(dialogAttributes.preferredRefreshRate - hostAttributes.preferredRefreshRate) <= RATE_EPSILON) return;
        dialogAttributes.preferredDisplayModeId = hostAttributes.preferredDisplayModeId;
        dialogAttributes.preferredRefreshRate = hostAttributes.preferredRefreshRate;
        dialogWindow.setAttributes(dialogAttributes);
    }

    private static void apply(@NonNull Activity activity, @NonNull android.view.Window window) {
        float configuredRate = PlayerSetting.getDefaultFrameRate();
        float preferredRate = findMode(activity, configuredRate) == null ? 0 : configuredRate;
        WindowManager.LayoutParams attributes = window.getAttributes();
        if (attributes.preferredDisplayModeId == 0 && Math.abs(attributes.preferredRefreshRate - preferredRate) <= RATE_EPSILON) return;
        attributes.preferredDisplayModeId = 0;
        attributes.preferredRefreshRate = preferredRate;
        window.setAttributes(attributes);
    }

    public static int getPreferredModeId(@NonNull Activity activity) {
        Display.Mode mode = findMode(activity, PlayerSetting.getDefaultFrameRate());
        return mode == null ? 0 : mode.getModeId();
    }

    @Nullable
    public static Display.Mode findMode(@NonNull Activity activity, float frameRate) {
        if (frameRate <= 0) return null;
        Display display = getDisplay(activity);
        if (display == null) return null;
        Display.Mode current = display.getMode();
        Display.Mode best = null;
        float bestDifference = Float.MAX_VALUE;
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() != current.getPhysicalWidth() || mode.getPhysicalHeight() != current.getPhysicalHeight()) continue;
            float difference = Math.abs(mode.getRefreshRate() - frameRate);
            if (difference <= RATE_EPSILON && difference < bestDifference) {
                best = mode;
                bestDifference = difference;
            }
        }
        return best;
    }

    @NonNull
    public static List<Float> getSupportedFrameRates(@NonNull Activity activity) {
        Display display = getDisplay(activity);
        List<Float> rates = new ArrayList<>();
        if (display == null) return rates;
        Display.Mode current = display.getMode();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() != current.getPhysicalWidth() || mode.getPhysicalHeight() != current.getPhysicalHeight()) continue;
            float rate = mode.getRefreshRate();
            boolean duplicate = false;
            for (float item : rates) {
                if (Math.abs(item - rate) <= RATE_EPSILON) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) rates.add(rate);
        }
        rates.sort(Comparator.naturalOrder());
        return rates;
    }

    @NonNull
    public static String getSettingText(@NonNull Activity activity) {
        float frameRate = PlayerSetting.getDefaultFrameRate();
        if (frameRate <= 0) return activity.getString(R.string.player_default_frame_rate_system);
        if (findMode(activity, frameRate) == null) return activity.getString(R.string.player_default_frame_rate_unavailable, formatRate(frameRate));
        return activity.getString(R.string.player_default_frame_rate_value, formatRate(frameRate));
    }

    @NonNull
    public static String formatRate(float frameRate) {
        if (Math.abs(frameRate - Math.round(frameRate)) < 0.005f) return String.format(Locale.getDefault(), "%d", Math.round(frameRate));
        return String.format(Locale.getDefault(), "%.3f", frameRate);
    }

    @Nullable
    private static Display getDisplay(@NonNull Activity activity) {
        Display display = activity.getWindow().getDecorView().getDisplay();
        if (display != null) return display;
        DisplayManager manager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
        return manager == null ? null : manager.getDisplay(Display.DEFAULT_DISPLAY);
    }

    @Nullable
    private static Activity findActivity(@NonNull Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private DefaultDisplayModeManager() {
    }
}
