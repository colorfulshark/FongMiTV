package com.fongmi.android.tv.player.exo;

import java.util.concurrent.TimeUnit;

final class SubtitleOffset {

    private volatile long offsetUs;

    public long getMs() {
        return TimeUnit.MICROSECONDS.toMillis(offsetUs);
    }

    public long getUs() {
        return offsetUs;
    }

    public void setMs(long offsetMs) {
        offsetUs = TimeUnit.MILLISECONDS.toMicros(offsetMs);
    }
}
