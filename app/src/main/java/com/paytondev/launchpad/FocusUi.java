package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native view styling; every pictorial glyph comes from the bundled icon assets. */
final class FocusUi {
    static final int BLUE = Color.rgb(0, 122, 255);
    static final int INK = Color.rgb(16, 21, 34);
    static final int MUTED = Color.rgb(103, 115, 136);
    static final int SURFACE = Color.rgb(240, 242, 246);
    static final int LINE = Color.rgb(223, 227, 234);
    final Context context;

    FocusUi(Context context) { this.context = context; }
    int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    LinearLayout column() { return linear(LinearLayout.VERTICAL); }
    LinearLayout row() { return linear(LinearLayout.HORIZONTAL); }
    private LinearLayout linear(int orientation) {
        LinearLayout v = new LinearLayout(context); v.setOrientation(orientation); return v;
    }
    TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(context);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }
    ImageView icon(int resource, int size) {
        ImageView v = new ImageView(context); v.setImageResource(resource);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return v;
    }
    GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(stroke == BLUE ? 2 : 1), stroke);
        return shape;
    }
    Drawable control(int fill, int radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, rounded(0xffe7f0ff, radius, BLUE));
        states.addState(new int[]{android.R.attr.state_activated}, fill == BLUE ? ring(fill, radius) : rounded(fill, radius, BLUE));
        states.addState(new int[]{}, rounded(fill, radius, 0));
        return states;
    }
    /** Solid fills of any color; the knob draws a blue ring outside the fill. */
    Drawable pill(int fill, int radius) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, ring(fill & 0xccffffff, radius));
        states.addState(new int[]{android.R.attr.state_activated}, ring(fill, radius));
        states.addState(new int[]{}, rounded(fill, radius, 0));
        return states;
    }
    private Drawable ring(int fill, int radius) {
        LayerDrawable ring = new LayerDrawable(new Drawable[]{rounded(Color.WHITE, radius, BLUE),
            rounded(fill, radius - 4, 0)});
        ring.setLayerInset(1, dp(4), dp(4), dp(4), dp(4));
        return ring;
    }
    @SuppressWarnings("deprecation")
    void background(View view, Drawable drawable) { view.setBackgroundDrawable(drawable); }
    View divider(boolean vertical) {
        View v = new View(context); v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(vertical ? dp(1) : -1, vertical ? -1 : dp(1)));
        return v;
    }
    FrameLayout appIcon(int resource, int color, int size, int glyphSize) {
        FrameLayout frame = new FrameLayout(context);
        background(frame, rounded(color, size >= 70 ? 20 : 13, 0));
        ImageView icon = icon(resource, glyphSize);
        frame.addView(icon, new FrameLayout.LayoutParams(dp(glyphSize), dp(glyphSize), Gravity.CENTER));
        return frame;
    }
    LinearLayout actionRow(String title, String subtitle, int resource, int fill, boolean large) {
        LinearLayout row = row(); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(large ? 8 : 24), dp(10), dp(16), dp(10));
        background(row, control(Color.WHITE, 16));
        int iconSize = large ? 84 : 56;
        row.addView(appIcon(resource, fill, iconSize, large ? 48 : 36),
            new LinearLayout.LayoutParams(dp(iconSize), dp(iconSize)));
        LinearLayout labels = column(); labels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1);
        labelParams.leftMargin = dp(large ? 24 : 28);
        labels.addView(text(title, large ? 28 : 22, INK, true));
        TextView sub = text(subtitle, large ? 19 : 18, MUTED, false);
        sub.setTag("subtitle"); sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(7); labels.addView(sub, subParams);
        row.addView(labels, labelParams); row.addView(icon(R.drawable.ic_chevron, 20));
        row.setContentDescription(title + ". " + subtitle);
        return row;
    }
    LinearLayout homeAction(String title, String subtitle, int resource, int fill) {
        LinearLayout row = row(); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(6), dp(8), dp(6));
        background(row, control(Color.WHITE, 16));
        row.addView(appIcon(resource, fill, 56, 34), new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout labels = column(); labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 24, INK, true);
        heading.setSingleLine(true); heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labels.addView(heading);
        TextView sub = text(subtitle, 16, MUTED, false); sub.setTag("subtitle");
        sub.setSingleLine(true); sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2); subParams.topMargin = dp(5);
        labels.addView(sub, subParams);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -1, 1);
        labelParams.leftMargin = dp(18); labelParams.rightMargin = dp(8);
        row.addView(labels, labelParams); row.addView(icon(R.drawable.ic_chevron, 20));
        row.setContentDescription(title + ". " + subtitle);
        return row;
    }
    LinearLayout button(String title, int resource, boolean outlined, int width) {
        LinearLayout row = row(); row.setGravity(Gravity.CENTER);
        row.setPadding(dp(12), 0, dp(12), 0);
        background(row, outlined ? outlinedControl() : control(SURFACE, 24));
        row.addView(icon(resource, 24));
        TextView label = text(title, 17, outlined ? BLUE : 0xff45536a, false);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
        p.leftMargin = dp(10); row.addView(label, p);
        row.setLayoutParams(new LinearLayout.LayoutParams(dp(width), dp(48)));
        row.setContentDescription(title);
        return row;
    }
    Drawable outlinedControl() {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, rounded(0xffe7f0ff, 24, BLUE));
        states.addState(new int[]{android.R.attr.state_activated}, rounded(0xffd9eaff, 24, BLUE));
        states.addState(new int[]{}, rounded(Color.WHITE, 24, BLUE));
        return states;
    }
    void available(View view, boolean available, String subtitle) {
        view.setEnabled(available); view.setAlpha(available ? 1f : 0.45f);
        TextView sub = (TextView) view.findViewWithTag("subtitle");
        if (sub != null) sub.setText(available ? subtitle : "Unavailable");
    }
    static TextView taggedText(ViewGroup parent, String tag) {
        return (TextView) parent.findViewWithTag(tag);
    }
}
