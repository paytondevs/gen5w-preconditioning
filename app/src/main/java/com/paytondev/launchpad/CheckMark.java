package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Drawn ✓ / ✗ / ? so the result never depends on head-unit font coverage. */
final class CheckMark extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float dp;
    private int state = PreconditioningSnapshot.UNKNOWN;

    CheckMark(Context context) {
        super(context); dp = context.getResources().getDisplayMetrics().density;
        paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setState(int value) { if (value != state) { state = value; invalidate(); } }
    static int color(int state) {
        return state == PreconditioningSnapshot.PASS ? 0xff1aa251 : state == PreconditioningSnapshot.FAIL ? 0xffe5484d : 0xffa0a7b2;
    }

    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight()), cx = getWidth() / 2f, cy = getHeight() / 2f, r = s / 2f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(color(state));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(0xffffffff); paint.setStrokeWidth(2.5f * dp);
        path.reset();
        if (state == PreconditioningSnapshot.PASS) {
            path.moveTo(cx - r * 0.42f, cy + r * 0.02f); path.lineTo(cx - r * 0.1f, cy + r * 0.34f); path.lineTo(cx + r * 0.44f, cy - r * 0.3f);
        } else if (state == PreconditioningSnapshot.FAIL) {
            path.moveTo(cx - r * 0.34f, cy - r * 0.34f); path.lineTo(cx + r * 0.34f, cy + r * 0.34f);
            path.moveTo(cx + r * 0.34f, cy - r * 0.34f); path.lineTo(cx - r * 0.34f, cy + r * 0.34f);
        } else {
            paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(r * 1.3f); paint.setFakeBoldText(true);
            canvas.drawText("?", cx, cy + r * 0.45f, paint); return;
        }
        paint.setStyle(Paint.Style.STROKE); canvas.drawPath(path, paint);
    }
}
