package com.fongmi.android.tv.player.model;

public final class MediaChapter {

    public final int index;
    public final long timeUs;
    public final String label;
    public final boolean selected;

    public MediaChapter(int index, long timeUs, String label, boolean selected) {
        this.index = index;
        this.timeUs = timeUs;
        this.label = label;
        this.selected = selected;
    }
}
