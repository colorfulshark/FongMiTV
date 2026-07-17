package com.fongmi.android.tv.ui.fragment;

import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentSettingPlayerBinding;
import com.fongmi.android.tv.impl.SpeedListener;
import com.fongmi.android.tv.impl.UaListener;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.SpeedDialog;
import com.fongmi.android.tv.ui.dialog.DisplayModeDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;

public class SettingPlayerFragment extends BaseFragment implements UaListener, SpeedListener {

    private FragmentSettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] background;
    private String[] autoFrameRate;
    private String[] caption;
    private String[] render;
    private String[] scale;

    public static SettingPlayerFragment newInstance() {
        return new SettingPlayerFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setVisible();
        setPlaybackModeText();
        format = new DecimalFormat("0.#");
        mBinding.speedText.setText(format.format(PlayerSetting.getSpeed()));
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
        mBinding.autoFrameRateText.setText((autoFrameRate = ResUtil.getStringArray(R.array.select_auto_frame_rate))[PlayerSetting.getAutoFrameRate()]);
        mBinding.displayModesText.setText(getString(R.string.player_display_modes_count, DisplayModeDialog.getModeCount(requireActivity())));
        mBinding.frameRateText.setText(Setting.getSwitch(PlayerSetting.isFrameRateVisible()));
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[PlayerSetting.getScale()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[PlayerSetting.isCaption() ? 1 : 0]);
        mBinding.backgroundText.setText((background = ResUtil.getStringArray(R.array.select_background))[PlayerSetting.getBackground()]);
    }

    @Override
    protected void initEvent() {
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.scale.setOnClickListener(this::onScale);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.autoFrameRate.setOnClickListener(this::onAutoFrameRate);
        mBinding.displayModes.setOnClickListener(this::onDisplayModes);
        mBinding.frameRate.setOnClickListener(this::onFrameRate);
        mBinding.adblock.setOnClickListener(this::setAdblock);
        mBinding.preload.setOnClickListener(this::onPreload);
        mBinding.decode.setOnClickListener(this::onDecode);
        mBinding.ua.setOnClickListener(this::onUa);
    }

    private void setVisible() {
        mBinding.caption.setVisibility(PlayerSetting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.displayModes.setVisibility(PlayerSetting.getAutoFrameRate() == PlayerSetting.AUTO_FRAME_RATE_OFF ? View.GONE : View.VISIBLE);
    }

    private void setRender(View view) {
        int index = (PlayerSetting.getRender() + 1) % render.length;
        PlayerSetting.putRender(index);
        setPlaybackModeText();
    }

    private void setPlaybackModeText() {
        render = ResUtil.getStringArray(R.array.select_render);
        mBinding.engineText.setText(R.string.play_exo);
        mBinding.renderText.setText(render[PlayerSetting.getRender()]);
    }

    private void onScale(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, PlayerSetting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            PlayerSetting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void setCaption(View view) {
        PlayerSetting.putCaption(!PlayerSetting.isCaption());
        mBinding.captionText.setText(caption[PlayerSetting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (PlayerSetting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return PlayerSetting.isCaption();
    }

    private void onSpeed(View view) {
        SpeedDialog.show(this);
    }

    @Override
    public void setSpeed(float speed) {
        mBinding.speedText.setText(format.format(speed));
        PlayerSetting.putSpeed(speed);
    }

    private void onBackground(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_background).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(background, PlayerSetting.getBackground(), (dialog, which) -> {
            mBinding.backgroundText.setText(background[which]);
            PlayerSetting.putBackground(which);
            dialog.dismiss();
        }).show();
    }

    private void onFrameRate(View view) {
        PlayerSetting.putFrameRateVisible(!PlayerSetting.isFrameRateVisible());
        mBinding.frameRateText.setText(Setting.getSwitch(PlayerSetting.isFrameRateVisible()));
    }

    private void onAutoFrameRate(View view) {
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_auto_frame_rate).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(autoFrameRate, PlayerSetting.getAutoFrameRate(), (dialog, which) -> {
            PlayerSetting.putAutoFrameRate(which);
            mBinding.autoFrameRateText.setText(autoFrameRate[which]);
            mBinding.displayModes.setVisibility(which == PlayerSetting.AUTO_FRAME_RATE_OFF ? View.GONE : View.VISIBLE);
            dialog.dismiss();
        }).show();
    }

    private void onDisplayModes(View view) {
        DisplayModeDialog.show(requireActivity());
    }

    private void setAdblock(View view) {
        Setting.putAdblock(!Setting.isAdblock());
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
    }

    private void onPreload(View view) {
        ((HomeActivity) requireActivity()).change(4);
    }

    private void onDecode(View view) {
        ((HomeActivity) requireActivity()).change(5);
    }

    private void onUa(View view) {
        UaDialog.show(this);
    }

    @Override
    public void setUa(String ua) {
        Setting.putUa(ua);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) initView();
    }
}
