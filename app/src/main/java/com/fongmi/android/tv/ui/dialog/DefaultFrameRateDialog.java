package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.DefaultDisplayModeManager;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public final class DefaultFrameRateDialog {

    public static void show(@NonNull Activity activity, @NonNull Runnable onChanged) {
        List<Float> rates = DefaultDisplayModeManager.getSupportedFrameRates(activity);
        float configured = PlayerSetting.getDefaultFrameRate();
        boolean configuredUnavailable = configured > 0 && DefaultDisplayModeManager.findMode(activity, configured) == null;
        CharSequence[] items = new CharSequence[rates.size() + 1 + (configuredUnavailable ? 1 : 0)];
        items[0] = activity.getString(R.string.player_default_frame_rate_system);
        int selected = 0;
        for (int i = 0; i < rates.size(); i++) {
            float rate = rates.get(i);
            items[i + 1] = activity.getString(R.string.player_default_frame_rate_value, DefaultDisplayModeManager.formatRate(rate));
            if (Math.abs(rate - configured) <= 0.02f) selected = i + 1;
        }
        if (configuredUnavailable) {
            selected = items.length - 1;
            items[selected] = activity.getString(R.string.player_default_frame_rate_unavailable, DefaultDisplayModeManager.formatRate(configured));
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.player_display_modes).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(items, selected, (selectedDialog, which) -> {
            if (which == 0) PlayerSetting.putDefaultFrameRate(0);
            else if (which <= rates.size()) PlayerSetting.putDefaultFrameRate(rates.get(which - 1));
            DefaultDisplayModeManager.apply(activity);
            onChanged.run();
            selectedDialog.dismiss();
        }).create();
        DefaultDisplayModeManager.inherit(activity, dialog);
        dialog.show();
    }

    private DefaultFrameRateDialog() {
    }
}
