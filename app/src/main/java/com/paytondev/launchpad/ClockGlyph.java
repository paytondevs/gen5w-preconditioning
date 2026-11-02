package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Drawn clock face for the schedule button; KitKat has no vector drawables. */
final class ClockGlyph extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private int color = 0xff45536a;

    ClockGlyph(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setColor(int value) { if (value != color) { color = value; invalidate(); } }

    @Override protected void onDraw(Canvas canvas) {
        float stroke = 2.5f * dp, cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - stroke;
        paint.setColor(color); paint.setStrokeWidth(stroke);
        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawLine(cx, cy, cx, cy - r * 0.58f, paint);            // minute hand at 12
        canvas.drawLine(cx, cy, cx + r * 0.42f, cy + r * 0.16f, paint); // hour hand just past 3
    }
}
