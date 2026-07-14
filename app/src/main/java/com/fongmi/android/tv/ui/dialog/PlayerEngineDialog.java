package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogPlayerEngineBinding;
import com.fongmi.android.tv.playback.PlaybackAction;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;

public final class PlayerEngineDialog extends BaseBottomSheetDialog {

    private DialogPlayerEngineBinding binding;
    private CharSequence title;

    public static void setText(TextView view) {
        setText(view, null);
    }

    public static void setText(TextView view, PlayerManager player) {
        if (view == null) return;
        view.setText(PlaybackAction.getEngineText(player));
    }

    public static void show(FragmentActivity activity, TextView view, PlayerManager player, CharSequence title) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof PlayerEngineDialog) return;
        PlayerEngineDialog dialog = new PlayerEngineDialog();
        dialog.title = title;
        dialog.show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogPlayerEngineBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.debug.setVisibility(View.GONE);
        binding.exo.setSelected(true);
        PlaybackActivity activity = getPlaybackActivity();
        binding.debug.setSelected(activity != null && activity.isDebugViewVisible());
        binding.exo.requestFocus();
    }

    @Override
    protected void initEvent() {
        binding.debug.setOnClickListener(this::selectDebug);
        binding.other.setOnClickListener(this::selectOther);
        binding.exo.setOnClickListener(view -> dismiss());
    }

    private void selectDebug(View view) {
        PlaybackActivity activity = getPlaybackActivity();
        if (activity == null) return;
        activity.toggleDebugView();
        view.setSelected(activity.isDebugViewVisible());
        dismiss();
    }

    private void selectOther(View view) {
        dismiss();
        PlaybackActivity activity = getPlaybackActivity();
        if (activity != null) activity.chooseOtherPlayer(title);
    }

    private PlaybackActivity getPlaybackActivity() {
        FragmentActivity activity = getActivity();
        return activity instanceof PlaybackActivity owner ? owner : null;
    }
}
