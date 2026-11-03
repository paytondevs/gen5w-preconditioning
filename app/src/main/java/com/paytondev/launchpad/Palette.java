package com.paytondev.launchpad;

/** Screen colors. DARK follows the head unit's night UI; LIGHT its day UI. */
final class Palette {
    final boolean dark;
    /** Window, cards, secondary buttons, and hairlines. */
    final int background, surface, raised, line;
    /** Primary text, secondary text, and disabled glyphs / unknown marks. */
    final int ink, muted, faint;
    /** Accent fill, text on it, and its soft tint for selected chips and pills. */
    final int accent, onAccent, accentSoft;
    /** TUNE-knob selection: a secondary control's fill and the accent fill. Never an outline. */
    final int focus, accentFocus, pressed;
    /** Battery cold, warming, and checks that passed or failed, with soft pill tints. */
    final int cold, warm, warmSoft, ok, danger;

    private Palette(boolean dark, int background, int surface, int raised, int line, int ink, int muted, int faint,
                    int accent, int onAccent, int accentSoft, int focus, int accentFocus, int pressed,
                    int cold, int warm, int warmSoft, int ok, int danger) {
        this.dark = dark; this.background = background; this.surface = surface; this.raised = raised;
        this.line = line; this.ink = ink; this.muted = muted; this.faint = faint;
        this.accent = accent; this.onAccent = onAccent; this.accentSoft = accentSoft;
        this.focus = focus; this.accentFocus = accentFocus; this.pressed = pressed;
        this.cold = cold; this.warm = warm; this.warmSoft = warmSoft; this.ok = ok; this.danger = danger;
    }

    static final Palette DARK = new Palette(true,
        0xff000000, 0xff121519, 0xff1b1f25, 0xff272c34,
        0xfff1f3f6, 0xff9fa8b6, 0xff59616d,
        0xff3fb3ea, 0xff001520, 0xff0f2a38,
        0xff1b3e55, 0xff8ad3f5, 0xff16303f,
        0xff5d8fd6, 0xffffa53a, 0xff33240f, 0xff3ccf8e, 0xffff6b6b);
    static final Palette LIGHT = new Palette(false,
        0xffeceef1, 0xffffffff, 0xfff4f5f7, 0xffd8dce2,
        0xff121820, 0xff4d5765, 0xffa3aab4,
        0xff0866ab, 0xffffffff, 0xffe3eff9,
        0xffc0d8ed, 0xff2f8fd6, 0xffd3e5f5,
        0xff3f72bd, 0xff9a5300, 0xfffbefdf, 0xff168a56, 0xffd13b3b);

    static Palette of(boolean dark) { return dark ? DARK : LIGHT; }

    /** WCAG contrast ratio between two opaque colors. */
    static double contrast(int a, int b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }
    private static double luminance(int color) {
        return 0.2126 * channel(color >> 16) + 0.7152 * channel(color >> 8) + 0.0722 * channel(color);
    }
    private static double channel(int value) {
        double c = (value & 0xff) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
