package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.ActivitySettingDecodeBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;

public class SettingDecodeActivity extends BaseActivity {

    private ActivitySettingDecodeBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingDecodeActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingDecodeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.tunnel.requestFocus();
        refresh();
    }

    @Override
    protected void initEvent() {
        mBinding.aac.setOnClickListener(this::setAAC);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.audioPassThrough.setOnClickListener(this::setAudioPassThrough);
    }

    private void refresh() {
        mBinding.aacText.setText(Setting.getSwitch(PlayerSetting.isPreferAAC()));
        mBinding.tunnelText.setText(Setting.getSwitch(PlayerSetting.isTunnel()));
        mBinding.audioPassThroughText.setText(Setting.getSwitch(PlayerSetting.isAudioPassThrough()));
    }

    private void setTunnel(View view) {
        PlayerSetting.putTunnel(!PlayerSetting.isTunnel());
        mBinding.tunnelText.setText(Setting.getSwitch(PlayerSetting.isTunnel()));
    }

    private void setAudioPassThrough(View view) {
        PlayerSetting.putAudioPassThrough(!PlayerSetting.isAudioPassThrough());
        mBinding.audioPassThroughText.setText(Setting.getSwitch(PlayerSetting.isAudioPassThrough()));
    }

    private void setAAC(View view) {
        PlayerSetting.putPreferAAC(!PlayerSetting.isPreferAAC());
        mBinding.aacText.setText(Setting.getSwitch(PlayerSetting.isPreferAAC()));
    }
}
