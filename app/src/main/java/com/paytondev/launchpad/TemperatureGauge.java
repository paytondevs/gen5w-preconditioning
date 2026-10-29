package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Threshold bar from -10°C to 35°C; the marker is the lowest pack reading. */
final class TemperatureGauge extends View {
    static final float MIN_C = -10, MAX_C = 35;
    static final int COLD = 0xff007aff, WARM = 0xfff59e0b; // COLD is Palette.LIGHT.accent as a host-testable literal
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final float dp;
    private Integer valueC;
    private int thresholdC = BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C;
    private Palette palette = Palette.LIGHT;

    TemperatureGauge(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        paint.setTextSize(13 * dp); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setValue(Integer celsius, int thresholdC) {
        if ((celsius == null ? valueC == null : celsius.equals(valueC)) && thresholdC == this.thresholdC) return;
        valueC = celsius; this.thresholdC = thresholdC; invalidate();
    }
    void setPalette(Palette value) { if (value != palette) { palette = value; invalidate(); } }
    static float fraction(float celsius) { return Math.max(0f, Math.min(1f, (celsius - MIN_C) / (MAX_C - MIN_C))); }
    static int color(Integer celsius, int thresholdC) { return BatteryTemperature.isWarm(celsius, thresholdC) ? WARM : COLD; }

    @Override protected void onDraw(Canvas canvas) {
        float pad = 10 * dp, left = pad, right = getWidth() - pad, width = right - left;
        float top = 22 * dp, bottom = top + 12 * dp, radius = 6 * dp;
        float threshold = left + width * fraction(thresholdC);
        track.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL); paint.setColor(palette.warmTrack);
        canvas.drawRoundRect(track, radius, radius, paint);
        canvas.save(); canvas.clipRect(left, top, threshold, bottom);
        paint.setColor(palette.coldTrack); canvas.drawRoundRect(track, radius, radius, paint); canvas.restore();
        if (valueC != null) {
            float x = left + width * fraction(valueC);
            canvas.save(); canvas.clipRect(left, top, x, bottom);
            paint.setColor(color(valueC, thresholdC)); canvas.drawRoundRect(track, radius, radius, paint); canvas.restore();
        }
        paint.setColor(palette.ink); paint.setStrokeWidth(2 * dp);
        canvas.drawLine(threshold, top - 4 * dp, threshold, bottom + 4 * dp, paint);
        paint.setTextAlign(Paint.Align.CENTER); paint.setFakeBoldText(true);
        canvas.drawText(BatteryTemperatureReading.fahrenheit(thresholdC) + "°F · " + thresholdC + "°C", threshold, top - 8 * dp, paint);
        paint.setFakeBoldText(false); paint.setColor(palette.muted);
        float labels = bottom + 18 * dp;
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(BatteryTemperatureReading.fahrenheit(MIN_C) + "°F", left, labels, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(BatteryTemperatureReading.fahrenheit(MAX_C) + "°F", right, labels, paint);
        if (valueC != null) {
            float x = left + width * fraction(valueC), cy = (top + bottom) / 2;
            paint.setColor(palette.card); canvas.drawCircle(x, cy, 10 * dp, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4 * dp); paint.setColor(color(valueC, thresholdC));
            canvas.drawCircle(x, cy, 8 * dp, paint);
        }
    }
}
