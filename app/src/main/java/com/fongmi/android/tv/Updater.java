package com.fongmi.android.tv;

import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.impl.UpdateListener;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.dialog.UpdateDialog;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Github;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;

public class Updater implements Download.Callback, UpdateListener {

    private Download download;
    private UpdateDialog dialog;

    private Updater() {
    }

    public static Updater create() {
        return new Updater();
    }

    private File getFile() {
        return Path.cache("update.apk");
    }

    private String getApkName() {
        return BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_abi + ".apk";
    }

    private Map<String, String> getHeaders() {
        return Map.of(HttpHeaders.ACCEPT, "application/vnd.github+json", HttpHeaders.USER_AGENT, "FongMiTV/" + BuildConfig.VERSION_NAME, "X-GitHub-Api-Version", "2022-11-28");
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Setting.putUpdate(true);
        return this;
    }

    public void start(FragmentActivity activity) {
        if (!Setting.getUpdate()) return;
        Task.execute(() -> doInBackground(activity));
    }

    private void doInBackground(FragmentActivity activity) {
        try {
            JSONObject object = new JSONObject(OkHttp.string(Github.getLatestRelease(), getHeaders()));
            String tag = object.optString("tag_name");
            if (tag.isEmpty()) throw new IllegalStateException("Release tag not found");
            if (!isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                App.post(() -> Notify.show(R.string.update_latest));
                return;
            }
            String releaseName = object.optString("name");
            String desc = object.optString("body");
            String apk = findApk(object.optJSONArray("assets"));
            String name = releaseName.isEmpty() ? tag : releaseName;
            if (apk.isEmpty()) throw new IllegalStateException("Release asset not found: " + getApkName());
            download = Download.create(apk, getFile());
            App.post(() -> show(activity, name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String findApk(JSONArray assets) {
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset != null && getApkName().equals(asset.optString("name"))) return asset.optString("browser_download_url");
        }
        return "";
    }

    static boolean isNewerVersion(String latest, String current) {
        try {
            String[] a = getVersionParts(latest);
            String[] b = getVersionParts(current);
            for (int i = 0; i < Math.max(a.length, b.length); i++) {
                int left = i < a.length ? Integer.parseInt(a[i]) : 0;
                int right = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (left != right) return left > right;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String[] getVersionParts(String version) {
        String value = version.trim();
        if (value.startsWith("v") || value.startsWith("V")) value = value.substring(1);
        int suffix = value.indexOf('-');
        if (suffix >= 0) value = value.substring(0, suffix);
        int metadata = value.indexOf('+');
        if (metadata >= 0) value = value.substring(0, metadata);
        return value.split("\\.");
    }

    private void show(FragmentActivity activity, String version, String desc) {
        dismiss();
        dialog = UpdateDialog.create().title(ResUtil.getString(R.string.update_version, version)).desc(desc).listener(this).show(activity);
    }

    @Override
    public void onConfirm(View view) {
        if (download == null) return;
        view.setEnabled(false);
        download.start(this);
    }

    @Override
    public void onCancel(View view) {
        Setting.putUpdate(false);
        if (download != null) download.cancel();
        dismiss();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        if (dialog != null) dialog.setProgress(progress);
    }

    @Override
    public void error(String msg) {
        Notify.show(msg);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(file);
        dismiss();
    }
}
