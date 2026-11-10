package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Threshold bar from -10°C to 35°C; the dot is the lowest pack reading. */
final class TemperatureGauge extends View {
    static final float MIN_C = -10, MAX_C = 35;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track = new RectF();
    private final float dp;
    private Integer valueC;
    private int thresholdC = BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C;
    private boolean heating;
    private BatteryDisplayUnit displayUnit = BatteryDisplayUnit.FAHRENHEIT;
    private Palette palette = Palette.DARK;

    TemperatureGauge(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        paint.setTextSize(13 * dp); paint.setTypeface(Fonts.get(context, Fonts.Weight.REGULAR));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setValue(Integer celsius, int thresholdC, boolean heating) {
        if ((celsius == null ? valueC == null : celsius.equals(valueC)) && thresholdC == this.thresholdC
            && heating == this.heating) return;
        valueC = celsius; this.thresholdC = thresholdC; this.heating = heating; invalidate();
    }
    void setDisplayUnit(BatteryDisplayUnit value) { if (value != displayUnit) { displayUnit = value; invalidate(); } }
    void setPalette(Palette value) { if (value != palette) { palette = value; invalidate(); } }
    static float fraction(float celsius) { return Math.max(0f, Math.min(1f, (celsius - MIN_C) / (MAX_C - MIN_C))); }

    @Override protected void onDraw(Canvas canvas) {
        float pad = 11 * dp, left = pad, right = getWidth() - pad, width = right - left;
        float top = 6 * dp, bottom = top + 10 * dp, radius = 5 * dp, cy = (top + bottom) / 2;
        float threshold = left + width * fraction(thresholdC);
        track.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL); paint.setColor(palette.warm);
        canvas.drawRoundRect(track, radius, radius, paint);
        canvas.save(); canvas.clipRect(left, top, threshold, bottom);
        paint.setColor(palette.cold); canvas.drawRoundRect(track, radius, radius, paint); canvas.restore();
        paint.setColor(palette.ink); paint.setAlpha(128); paint.setStrokeWidth(2 * dp);
        canvas.drawLine(threshold, top - 6 * dp, threshold, bottom + 6 * dp, paint); paint.setAlpha(255);

        float labels = bottom + 24 * dp;
        paint.setColor(palette.muted);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(displayUnit.format(MIN_C), left, labels, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(displayUnit.format(MAX_C), right, labels, paint);
        String warmAt = "Warm at " + displayUnit.format(thresholdC);
        float half = paint.measureText(warmAt) / 2, edge = 48 * dp;
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(warmAt, Math.max(left + edge + half, Math.min(right - edge - half, threshold)), labels, paint);

        if (valueC != null) {
            float x = left + width * fraction(valueC);
            paint.setColor(palette.background); canvas.drawCircle(x, cy, 11 * dp, paint);
            paint.setColor(heating ? palette.warm : palette.ink); canvas.drawCircle(x, cy, 7 * dp, paint);
        }
    }
}
