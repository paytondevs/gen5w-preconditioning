package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

/** Session trace of received readings; breaks where polling stopped, never interpolates. */
final class TemperatureChart extends View {
    static final long MIN_SPAN_MS = 5 * 60 * 1000L;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashed = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF frame = new RectF();
    private final float dp;
    private TemperatureHistory.Trace trace = TemperatureHistory.Trace.EMPTY;
    private long now;
    private int thresholdC = BatteryTemperature.EXPECTED_HEATING_THRESHOLD_C;
    private Palette palette = Palette.LIGHT;

    TemperatureChart(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        line.setStyle(Paint.Style.STROKE); line.setStrokeCap(Paint.Cap.ROUND); line.setStrokeJoin(Paint.Join.ROUND);
        dashed.setStyle(Paint.Style.STROKE); dashed.setStrokeWidth(2 * dp); dashed.setColor(TemperatureGauge.WARM);
        dashed.setPathEffect(new DashPathEffect(new float[]{8 * dp, 6 * dp}, 0));
        label.setTextSize(13 * dp); label.setColor(palette.muted);
        setContentDescription("Battery temperature this session");
    }
    void setPalette(Palette value) {
        if (value == palette) return;
        palette = value; label.setColor(palette.muted); invalidate();
    }
    void setTrace(TemperatureHistory.Trace trace, long now, int thresholdC) {
        this.trace = trace; this.now = now; this.thresholdC = thresholdC; invalidate();
    }

    /** Visible time span: at least five minutes, at most the history window. */
    static long start(List<TemperatureHistory.Sample> samples, long now) {
        long span = samples.isEmpty() ? MIN_SPAN_MS : now - samples.get(0).at;
        return now - Math.max(MIN_SPAN_MS, Math.min(TemperatureHistory.WINDOW_MS, span));
    }
    /** °C bounds that always include the threshold, padded, spanning at least 10°C. */
    static float[] range(List<TemperatureHistory.Sample> samples, int thresholdC) {
        float lo = thresholdC, hi = lo;
        for (TemperatureHistory.Sample s : samples) {
            lo = Math.min(lo, s.minimumC); hi = Math.max(hi, s.maximumC == null ? s.minimumC : s.maximumC);
        }
        lo -= 2; hi += 2;
        if (hi - lo < 10) { float mid = (lo + hi) / 2; lo = mid - 5; hi = mid + 5; }
        return new float[]{lo, hi};
    }
    static float x(long at, long start, long end, float left, float width) {
        return end <= start ? left + width : left + width * Math.max(0f, Math.min(1f, (at - start) / (float) (end - start)));
    }
    static float y(float celsius, float lo, float hi, float top, float height) {
        return top + height * (1f - Math.max(0f, Math.min(1f, (celsius - lo) / (hi - lo))));
    }

    @Override protected void onDraw(Canvas canvas) {
        frame.set(0, 0, getWidth(), getHeight());
        fill.setStyle(Paint.Style.FILL); fill.setColor(palette.card);
        canvas.drawRoundRect(frame, 14 * dp, 14 * dp, fill);
        List<TemperatureHistory.Sample> samples = trace.samples;
        if (samples.isEmpty()) {
            label.setTextAlign(Paint.Align.CENTER); label.setTextSize(17 * dp);
            canvas.drawText("Waiting for readings", getWidth() / 2f, getHeight() / 2f + 6 * dp, label);
            label.setTextSize(13 * dp); return;
        }
        float left = 48 * dp, top = 14 * dp, width = getWidth() - left - 14 * dp, height = getHeight() - top - 28 * dp;
        float[] range = range(samples, thresholdC);
        long start = start(samples, now);
        label.setTextAlign(Paint.Align.RIGHT);
        line.setStrokeWidth(dp); line.setColor(palette.line);
        for (float c : range) {
            float yy = y(c, range[0], range[1], top, height);
            canvas.drawLine(left, yy, left + width, yy, line);
            canvas.drawText(BatteryTemperatureReading.fahrenheit(c) + "°F", left - 8 * dp, yy + 5 * dp, label);
        }
        float threshold = y(thresholdC, range[0], range[1], top, height);
        path.reset(); path.moveTo(left, threshold); path.lineTo(left + width, threshold);
        canvas.drawPath(path, dashed);
        label.setColor(TemperatureGauge.WARM); label.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(BatteryTemperatureReading.fahrenheit(thresholdC) + "°F", left + width, threshold - 6 * dp, label);
        label.setColor(palette.muted);
        // The session marker's label sits in the time axis row, replacing an end label it would hit.
        float axis = top + height + 20 * dp, sx = -1;
        if (trace.sessionStart >= 0 && trace.sessionStart >= start) {
            sx = x(trace.sessionStart, start, now, left, width);
            line.setColor(palette.ink); line.setStrokeWidth(1.5f * dp);
            canvas.drawLine(sx, top, sx, top + height + 6 * dp, line);
            label.setTextAlign(Paint.Align.CENTER); label.setColor(palette.ink); label.setFakeBoldText(true);
            canvas.drawText("Start", Math.max(left + 18 * dp, Math.min(left + width - 18 * dp, sx)), axis, label);
            label.setColor(palette.muted); label.setFakeBoldText(false);
        }
        trace(canvas, samples, true, range, start, left, top, width, height);
        trace(canvas, samples, false, range, start, left, top, width, height);
        label.setTextAlign(Paint.Align.LEFT);
        if (sx < 0 || sx > left + 90 * dp)
            canvas.drawText("−" + Math.round((now - start) / 60000f) + " min", left, axis, label);
        label.setTextAlign(Paint.Align.RIGHT);
        if (sx < 0 || sx < left + width - 70 * dp) canvas.drawText("Now", left + width, axis, label);
    }
    private void trace(Canvas canvas, List<TemperatureHistory.Sample> samples, boolean maximum, float[] range,
                       long start, float left, float top, float width, float height) {
        int color = maximum ? 0x99f59e0b : palette.accent;
        line.setColor(color); line.setStrokeWidth((maximum ? 2 : 3) * dp); fill.setColor(color);
        path.reset();
        TemperatureHistory.Sample previous = null;
        for (int i = 0; i < samples.size(); i++) {
            TemperatureHistory.Sample s = samples.get(i);
            Integer value = maximum ? s.maximumC : Integer.valueOf(s.minimumC);
            if (value == null || s.at < start) { previous = null; continue; }
            float px = x(s.at, start, now, left, width), py = y(value, range[0], range[1], top, height);
            boolean joined = previous != null && s.at - previous.at <= TemperatureHistory.GAP_MS;
            if (joined) path.lineTo(px, py); else path.moveTo(px, py);
            TemperatureHistory.Sample next = i + 1 < samples.size() ? samples.get(i + 1) : null;
            boolean isolated = !joined && (next == null || next.at - s.at > TemperatureHistory.GAP_MS
                || (maximum && next.maximumC == null));
            if (isolated || next == null) canvas.drawCircle(px, py, (maximum ? 3 : 4.5f) * dp, fill);
            previous = s;
        }
        canvas.drawPath(path, line);
    }
}
