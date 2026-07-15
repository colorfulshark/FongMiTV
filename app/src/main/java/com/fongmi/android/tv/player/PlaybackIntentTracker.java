package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;
import androidx.media3.common.Player;

import java.util.Map;
import java.util.WeakHashMap;

/** Tracks explicit play/pause requests without treating AFR's temporary pause as user intent. */
public final class PlaybackIntentTracker {

    private static final Map<Player, Long> VERSIONS = new WeakHashMap<>();

    private PlaybackIntentTracker() {
    }

    public static synchronized long getVersion(@NonNull Player player) {
        return VERSIONS.getOrDefault(player, 0L);
    }

    public static synchronized void record(@NonNull Player player) {
        VERSIONS.put(player, getVersion(player) + 1);
    }
}
