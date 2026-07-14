package com.fongmi.android.tv.player.danmaku;

/** App-owned danmaku preferences, independent from the Media3 API surface. */
public final class DanmakuConfig {

    public static final int STYLE_NONE = 0;
    public static final int STYLE_SHADOW = 1;
    public static final int STYLE_STROKE = 2;
    public static final int STYLE_PROJECTION = 3;
    public static final int COLOR_MODE_DEFAULT = 0;
    public static final int COLOR_MODE_COLORFUL = 1;
    public static final int COLOR_MODE_GRADIENT = 2;
    public static final DanmakuConfig DEFAULT = new Builder().build();

    public final float textScale;
    public final float transparency;
    public final boolean textBold;
    public final int styleMode;
    public final float shadowTransparency;
    public final float strokeWidthMultiplier;
    public final float projectionOffsetXMultiplier;
    public final float projectionOffsetYMultiplier;
    public final float projectionTransparency;
    public final int colorMode;
    public final long durationMs;
    public final long fixedDurationMs;
    public final long timeOffsetMs;
    public final int maxOnScreen;
    public final float scrollAreaRatio;
    public final float scrollGapRatio;
    public final float lineSpacing;
    public final int maxScrollLines;
    public final int maxTopLines;
    public final int maxBottomLines;
    public final boolean showScroll;
    public final boolean showTop;
    public final boolean showBottom;
    public final boolean showReverse;
    public final boolean showPositioned;
    public final boolean showSubtitle;
    public final boolean showSpecial;

    private DanmakuConfig(Builder b) {
        textScale = b.textScale;
        transparency = b.transparency;
        textBold = b.textBold;
        styleMode = b.styleMode;
        shadowTransparency = b.shadowTransparency;
        strokeWidthMultiplier = b.strokeWidthMultiplier;
        projectionOffsetXMultiplier = b.projectionOffsetXMultiplier;
        projectionOffsetYMultiplier = b.projectionOffsetYMultiplier;
        projectionTransparency = b.projectionTransparency;
        colorMode = b.colorMode;
        durationMs = b.durationMs;
        fixedDurationMs = b.fixedDurationMs;
        timeOffsetMs = b.timeOffsetMs;
        maxOnScreen = b.maxOnScreen;
        scrollAreaRatio = b.scrollAreaRatio;
        scrollGapRatio = b.scrollGapRatio;
        lineSpacing = b.lineSpacing;
        maxScrollLines = b.maxScrollLines;
        maxTopLines = b.maxTopLines;
        maxBottomLines = b.maxBottomLines;
        showScroll = b.showScroll;
        showTop = b.showTop;
        showBottom = b.showBottom;
        showReverse = b.showReverse;
        showPositioned = b.showPositioned;
        showSubtitle = b.showSubtitle;
        showSpecial = b.showSpecial;
    }

    public static final class Builder {
        private float textScale = 1f;
        private float transparency;
        private boolean textBold;
        private int styleMode = STYLE_STROKE;
        private float shadowTransparency = .1f;
        private float strokeWidthMultiplier = .12f;
        private float projectionOffsetXMultiplier = .08f;
        private float projectionOffsetYMultiplier = .08f;
        private float projectionTransparency = .2f;
        private int colorMode = COLOR_MODE_DEFAULT;
        private long durationMs = 8000;
        private long fixedDurationMs = 5000;
        private long timeOffsetMs;
        private int maxOnScreen = 150;
        private float scrollAreaRatio = .5f;
        private float scrollGapRatio;
        private float lineSpacing = 1.4f;
        private int maxScrollLines;
        private int maxTopLines;
        private int maxBottomLines;
        private boolean showScroll = true;
        private boolean showTop = true;
        private boolean showBottom = true;
        private boolean showReverse = true;
        private boolean showPositioned = true;
        private boolean showSubtitle = true;
        private boolean showSpecial = true;

        public Builder setTextScale(float v) { textScale = v; return this; }
        public Builder setTransparency(float v) { transparency = v; return this; }
        public Builder setTextBold(boolean v) { textBold = v; return this; }
        public Builder setStyleMode(int v) { styleMode = v; return this; }
        public Builder setShadowTransparency(float v) { shadowTransparency = v; return this; }
        public Builder setStrokeWidthMultiplier(float v) { strokeWidthMultiplier = v; return this; }
        public Builder setProjectionOffsetXMultiplier(float v) { projectionOffsetXMultiplier = v; return this; }
        public Builder setProjectionOffsetYMultiplier(float v) { projectionOffsetYMultiplier = v; return this; }
        public Builder setProjectionTransparency(float v) { projectionTransparency = v; return this; }
        public Builder setColorMode(int v) { colorMode = v; return this; }
        public Builder setDurationMs(long v) { durationMs = v; return this; }
        public Builder setFixedDurationMs(long v) { fixedDurationMs = v; return this; }
        public Builder setTimeOffsetMs(long v) { timeOffsetMs = v; return this; }
        public Builder setMaxOnScreen(int v) { maxOnScreen = v; return this; }
        public Builder setScrollAreaRatio(float v) { scrollAreaRatio = v; return this; }
        public Builder setScrollGapRatio(float v) { scrollGapRatio = v; return this; }
        public Builder setLineSpacing(float v) { lineSpacing = v; return this; }
        public Builder setMaxScrollLines(int v) { maxScrollLines = v; return this; }
        public Builder setMaxTopLines(int v) { maxTopLines = v; return this; }
        public Builder setMaxBottomLines(int v) { maxBottomLines = v; return this; }
        public Builder setShowScroll(boolean v) { showScroll = v; return this; }
        public Builder setShowTop(boolean v) { showTop = v; return this; }
        public Builder setShowBottom(boolean v) { showBottom = v; return this; }
        public Builder setShowReverse(boolean v) { showReverse = v; return this; }
        public Builder setShowPositioned(boolean v) { showPositioned = v; return this; }
        public Builder setShowSubtitle(boolean v) { showSubtitle = v; return this; }
        public Builder setShowSpecial(boolean v) { showSpecial = v; return this; }
        public DanmakuConfig build() { return new DanmakuConfig(this); }
    }
}
