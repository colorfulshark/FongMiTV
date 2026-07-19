package com.fongmi.android.tv.utils;

public class Github {

    private static final String API = "https://api.github.com/repos/colorfulshark/FongMiTV";

    public static String getLatestRelease() {
        return API + "/releases/latest";
    }
}
