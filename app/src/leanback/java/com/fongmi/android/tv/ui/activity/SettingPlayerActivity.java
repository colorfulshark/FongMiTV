package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.SpeedListener;
import com.fongmi.android.tv.impl.UaListener;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SpeedDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;

import java.text.DecimalFormat;

public class SettingPlayerActivity extends BaseActivity implements UaListener, SpeedListener {

    private ActivitySettingPlayerBinding mBinding;
    private DecimalFormat format;
    private String[] caption;
    private String[] autoFrameRate;
    private String[] render;
    private String[] scale;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setVisible();
        setPlaybackModeText();
        mBinding.engine.requestFocus();
        format = new DecimalFormat("0.#");
        mBinding.speedText.setText(format.format(PlayerSetting.getSpeed()));
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
        mBinding.backgroundText.setText(Setting.getSwitch(PlayerSetting.isBackgroundOn()));
        mBinding.autoFrameRateText.setText((autoFrameRate = ResUtil.getStringArray(R.array.select_auto_frame_rate))[PlayerSetting.getAutoFrameRate()]);
        mBinding.frameRateText.setText(Setting.getSwitch(PlayerSetting.isFrameRateVisible()));
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[PlayerSetting.getScale()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[PlayerSetting.isCaption() ? 1 : 0]);
    }

    @Override
    protected void initEvent() {
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.scale.setOnClickListener(this::setScale);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.speed.setOnClickListener(this::onSpeed);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.autoFrameRate.setOnClickListener(this::onAutoFrameRate);
        mBinding.frameRate.setOnClickListener(this::onFrameRate);
        mBinding.adblock.setOnClickListener(this::setAdblock);
        mBinding.preload.setOnClickListener(this::onPreloadSetting);
        mBinding.decode.setOnClickListener(this::onDecodeSetting);
        mBinding.ua.setOnClickListener(this::onUa);
    }

    private void setVisible() {
        if (PlayerSetting.isBackgroundPiP()) PlayerSetting.putBackground(1);
        mBinding.caption.setVisibility(PlayerSetting.hasCaption() ? View.VISIBLE : View.GONE);
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

    private void setScale(View view) {
        int index = (PlayerSetting.getScale() + 1) % scale.length;
        mBinding.scaleText.setText(scale[index]);
        PlayerSetting.putScale(index);
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
        PlayerSetting.putBackground(PlayerSetting.isBackgroundOn() ? 0 : 1);
        mBinding.backgroundText.setText(Setting.getSwitch(PlayerSetting.isBackgroundOn()));
    }

    private void onFrameRate(View view) {
        PlayerSetting.putFrameRateVisible(!PlayerSetting.isFrameRateVisible());
        mBinding.frameRateText.setText(Setting.getSwitch(PlayerSetting.isFrameRateVisible()));
    }

    private void onAutoFrameRate(View view) {
        int mode = (PlayerSetting.getAutoFrameRate() + 1) % autoFrameRate.length;
        PlayerSetting.putAutoFrameRate(mode);
        mBinding.autoFrameRateText.setText(autoFrameRate[mode]);
    }

    private void setAdblock(View view) {
        Setting.putAdblock(!Setting.isAdblock());
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
    }

    private void onPreloadSetting(View view) {
        SettingPreloadActivity.start(this);
    }

    private void onDecodeSetting(View view) {
        SettingDecodeActivity.start(this);
    }

    private void onUa(View view) {
        UaDialog.show(this);
    }

    @Override
    public void setUa(String ua) {
        Setting.putUa(ua);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBinding != null) setPlaybackModeText();
    }
}
