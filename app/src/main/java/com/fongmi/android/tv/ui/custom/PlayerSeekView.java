package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

public final class PlayerSeekView extends FrameLayout {

    private final DefaultTimeBar timeBar;
    private final Runnable updater = this::updateProgress;
    private Player player;

    public PlayerSeekView(@NonNull Context context) {
        this(context, null);
    }

    public PlayerSeekView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        timeBar = new DefaultTimeBar(context, attrs);
        timeBar.setId(androidx.media3.ui.R.id.exo_progress);
        addView(timeBar, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        timeBar.addListener(new TimeBar.OnScrubListener() {
            @Override public void onScrubStart(@NonNull TimeBar bar, long position) {}
            @Override public void onScrubMove(@NonNull TimeBar bar, long position) {}
            @Override public void onScrubStop(@NonNull TimeBar bar, long position, boolean canceled) {
                if (!canceled && player != null && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) player.seekTo(position);
            }
        });
    }

    public void setPlayer(@Nullable Player player) {
        this.player = player;
        updateProgress();
    }

    public TimeBar getTimeBar() {
        return timeBar;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateProgress();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(updater);
        super.onDetachedFromWindow();
    }

    private void updateProgress() {
        removeCallbacks(updater);
        long duration = player == null ? 0 : player.getDuration();
        timeBar.setDuration(duration == C.TIME_UNSET ? 0 : Math.max(0, duration));
        timeBar.setPosition(player == null ? 0 : Math.max(0, player.getCurrentPosition()));
        timeBar.setBufferedPosition(player == null ? 0 : Math.max(0, player.getBufferedPosition()));
        if (isAttachedToWindow() && player != null) postDelayed(updater, 500);
    }
}
