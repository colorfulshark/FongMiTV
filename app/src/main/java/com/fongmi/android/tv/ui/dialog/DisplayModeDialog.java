package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.Comparator;

public final class DisplayModeDialog {

    public static void show(@NonNull Activity activity) {
        Display display = getDisplay(activity);
        Display.Mode[] modes = display == null ? new Display.Mode[0] : display.getSupportedModes().clone();
        Arrays.sort(modes, Comparator.comparingInt(Display.Mode::getModeId));
        int currentModeId = display == null ? 0 : display.getMode().getModeId();
        CharSequence[] items = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) {
            Display.Mode mode = modes[i];
            String current = mode.getModeId() == currentModeId ? activity.getString(R.string.player_display_mode_current) : "";
            items[i] = activity.getString(R.string.player_display_mode_value, mode.getModeId(), mode.getPhysicalWidth(), mode.getPhysicalHeight(), mode.getRefreshRate(), current);
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity).setTitle(R.string.player_display_modes).setPositiveButton(R.string.dialog_positive, null);
        if (items.length == 0) builder.setMessage(R.string.player_display_modes_empty);
        else builder.setItems(items, null);
        builder.show();
    }

    public static int getModeCount(@NonNull Activity activity) {
        Display display = getDisplay(activity);
        return display == null ? 0 : display.getSupportedModes().length;
    }

    private static Display getDisplay(@NonNull Activity activity) {
        Display display = activity.getWindow().getDecorView().getDisplay();
        if (display != null) return display;
        DisplayManager manager = (DisplayManager) activity.getSystemService(Context.DISPLAY_SERVICE);
        return manager == null ? null : manager.getDisplay(Display.DEFAULT_DISPLAY);
    }

    private DisplayModeDialog() {
    }
}
