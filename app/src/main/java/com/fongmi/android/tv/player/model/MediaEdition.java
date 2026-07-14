package com.fongmi.android.tv.player.model;

public final class MediaEdition {

    public final int index;
    public final long durationUs;
    public final String label;
    public final boolean selected;

    public MediaEdition(int index, long durationUs, String label, boolean selected) {
        this.index = index;
        this.durationUs = durationUs;
        this.label = label;
        this.selected = selected;
    }
}
