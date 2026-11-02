package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Native view styling. The TUNE knob selects a control by setting it activated, and every
 * control shows that as a change of fill, never an outline.
 */
final class FocusUi {
    final Context context;
    final Palette p;

    FocusUi(Context context, Palette palette) { this.context = context; this.p = palette; }
    int dp(float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    LinearLayout column() { return linear(LinearLayout.VERTICAL); }
    LinearLayout row() { return linear(LinearLayout.HORIZONTAL); }
    private LinearLayout linear(int orientation) {
        LinearLayout v = new LinearLayout(context); v.setOrientation(orientation); return v;
    }
    TextView text(String value, int size, int color, Fonts.Weight weight) {
        TextView v = new TextView(context);
        v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(Fonts.get(context, weight));
        v.setIncludeFontPadding(false);
        return v;
    }
    TextView text(String value, int size, int color, boolean bold) {
        return text(value, size, color, bold ? Fonts.Weight.MEDIUM : Fonts.Weight.REGULAR);
    }
    /** Small uppercase section label. */
    TextView caption(String value) {
        TextView v = text(value.toUpperCase(java.util.Locale.US), 13, p.muted, Fonts.Weight.SEMI_BOLD);
        v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END); return v;
    }
    GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(radius));
        if (stroke != 0) shape.setStroke(dp(1), stroke);
        return shape;
    }
    private Drawable states(Drawable pressed, Drawable activated, Drawable normal) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, pressed);
        states.addState(new int[]{android.R.attr.state_activated}, activated);
        states.addState(new int[]{}, normal);
        return states;
    }
    /** Accent-filled main action. */
    Drawable primary(int radius) {
        return states(rounded(p.accentFocus, radius, 0), rounded(p.accentFocus, radius, 0), rounded(p.accent, radius, 0));
    }
    /** Raised secondary action with a hairline. */
    Drawable secondary(int radius) {
        return states(rounded(p.pressed, radius, 0), rounded(p.focus, radius, 0), rounded(p.raised, radius, p.line));
    }
    /** List rows and menu items: no fill until pressed or selected; {@code idle} for a selected menu page. */
    Drawable quiet(int idle, int radius) {
        return states(rounded(p.pressed, radius, 0), rounded(p.focus, radius, 0), rounded(idle, radius, 0));
    }
    TextView primaryButton(String label) {
        TextView v = text(label, 21, p.onAccent, Fonts.Weight.MEDIUM); v.setGravity(Gravity.CENTER);
        background(v, primary(12)); return v;
    }
    TextView secondaryButton(String label, int color) {
        TextView v = text(label, 21, color, Fonts.Weight.MEDIUM); v.setGravity(Gravity.CENTER);
        v.setPadding(dp(24), 0, dp(24), 0); background(v, secondary(12)); return v;
    }
    @SuppressWarnings("deprecation")
    void background(View view, Drawable drawable) { view.setBackgroundDrawable(drawable); }
    View divider(boolean vertical) {
        View v = new View(context); v.setBackgroundColor(p.line);
        v.setLayoutParams(new LinearLayout.LayoutParams(vertical ? dp(1) : -1, vertical ? -1 : dp(1)));
        return v;
    }
    /** Card: surface fill, hairline, 16 dp corners. */
    LinearLayout card() {
        LinearLayout v = column(); v.setPadding(dp(20), dp(18), dp(20), dp(18));
        background(v, rounded(p.surface, 16, p.line)); return v;
    }
    /** Rounded status pill with a leading dot; {@link #setPill} restyles it. */
    TextView pill() {
        TextView v = text("", 16, p.ink, Fonts.Weight.MEDIUM); v.setGravity(Gravity.CENTER_VERTICAL);
        v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END);
        v.setPadding(dp(14), 0, dp(16), 0); v.setCompoundDrawablePadding(dp(9));
        return v;
    }
    void setPill(TextView pill, String label, int color, int fill) {
        pill.setText(label); pill.setTextColor(color); background(pill, rounded(fill, 16, 0));
        GradientDrawable dot = rounded(color, 5, 0); dot.setBounds(0, 0, dp(9), dp(9));
        pill.setCompoundDrawables(dot, null, null, null);
    }
    /** "Label ............ value ›" settings row; returns the value view via tag "value". */
    LinearLayout listRow(String label, String value, boolean chevron) {
        LinearLayout row = row(); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(12), 0); background(row, quiet(0, 10));
        TextView title = text(label, 18, p.ink, Fonts.Weight.REGULAR); title.setSingleLine(true);
        row.addView(title);
        TextView v = text(value, 16, p.muted, Fonts.Weight.REGULAR); v.setTag("value");
        v.setGravity(Gravity.RIGHT); v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, -2, 1); valueParams.leftMargin = dp(16);
        row.addView(v, valueParams);
        if (chevron) {
            MenuGlyph more = new MenuGlyph(context, MenuGlyph.Kind.MORE); more.setColor(p.faint);
            LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(18), dp(18)); moreParams.leftMargin = dp(10);
            row.addView(more, moreParams);
        }
        row.setContentDescription(label);
        return row;
    }
    void available(View view, boolean available) {
        view.setEnabled(available); view.setAlpha(available ? 1f : 0.45f);
    }
}
