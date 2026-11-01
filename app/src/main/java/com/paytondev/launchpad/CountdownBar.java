package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Unlabeled bar that drains over DURATION_MS from an elapsedRealtime start. */
final class CountdownBar extends View {
    static final long DURATION_MS = 90 * 1000L;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final float dp;
    private long startedAt = -1;
    private Palette palette = Palette.LIGHT;

    CountdownBar(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    /** -1 clears the countdown. */
    void setStartedAt(long value) { if (value != startedAt) { startedAt = value; invalidate(); } }
    void setPalette(Palette value) { if (value != palette) { palette = value; invalidate(); } }
    static float remaining(long startedAt, long now) {
        return Math.max(0f, Math.min(1f, 1f - (float) (now - startedAt) / DURATION_MS));
    }

    @Override protected void onDraw(Canvas canvas) {
        float height = 8 * dp, top = (getHeight() - height) / 2, radius = height / 2;
        track.set(0, top, getWidth(), top + height);
        paint.setColor(palette.line); canvas.drawRoundRect(track, radius, radius, paint);
        if (startedAt < 0) return;
        float fraction = remaining(startedAt, SystemClock.elapsedRealtime());
        if (fraction <= 0) return;
        canvas.save(); canvas.clipRect(0, top, getWidth() * fraction, top + height);
        paint.setColor(palette.accent); canvas.drawRoundRect(track, radius, radius, paint); canvas.restore();
        postInvalidateOnAnimation();
    }
}
