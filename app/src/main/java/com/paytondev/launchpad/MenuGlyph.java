package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Drawn line icons for the menu, buttons and list rows; KitKat has no vector drawables. */
final class MenuGlyph extends View {
    enum Kind { BACK, MORE, BATTERY, SCHEDULE }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private final float dp;
    private final Kind kind;
    private int color = 0xff8b94a2;

    MenuGlyph(Context context, Kind kind) {
        super(context); this.kind = kind; dp = context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setColor(int value) { if (value != color) { color = value; invalidate(); } }

    @Override protected void onDraw(Canvas canvas) {
        float stroke = 2.2f * dp, cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(getWidth(), getHeight()) / 2f - stroke;
        paint.setColor(color); paint.setStrokeWidth(stroke); paint.setStyle(Paint.Style.STROKE);
        switch (kind) {
            case BACK: case MORE: {
                float d = kind == Kind.BACK ? -1 : 1, w = r * 0.42f, h = r * 0.8f;
                canvas.drawLine(cx - d * w, cy - h, cx + d * w, cy, paint);
                canvas.drawLine(cx + d * w, cy, cx - d * w, cy + h, paint);
                break;
            }
            case BATTERY: {
                float w = r * 0.62f, top = cy - r * 0.78f, bottom = cy + r;
                box.set(cx - w, top, cx + w, bottom);
                canvas.drawRoundRect(box, 3 * dp, 3 * dp, paint);
                canvas.drawLine(cx - w * 0.4f, top - 2.5f * dp, cx + w * 0.4f, top - 2.5f * dp, paint);
                paint.setStyle(Paint.Style.FILL);
                box.set(cx - w + 3.5f * dp, cy + r * 0.15f, cx + w - 3.5f * dp, bottom - 3.5f * dp);
                canvas.drawRect(box, paint);
                break;
            }
            case SCHEDULE:
                canvas.drawCircle(cx, cy, r, paint);
                canvas.drawLine(cx, cy, cx, cy - r * 0.58f, paint);
                canvas.drawLine(cx, cy, cx + r * 0.42f, cy + r * 0.16f, paint);
                break;
        }
    }
}
