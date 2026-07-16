package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.FragmentSettingDecodeBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseFragment;

public class SettingDecodeFragment extends BaseFragment {

    private FragmentSettingDecodeBinding mBinding;

    public static SettingDecodeFragment newInstance() {
        return new SettingDecodeFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingDecodeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) refresh();
    }
}
