package com.paytondev.launchpad;

/** Screen colors. LIGHT is the original look; DARK follows the head unit's night UI. */
final class Palette {
    final boolean dark;
    /** Window, raised panels, cards inside panels, and hairlines. */
    final int background, surface, card, line;
    /** Primary text, secondary text, and labels on secondary buttons. */
    final int ink, muted, secondary;
    /** Accent fill, text on it, accent-colored text, and its pressed / knob-activated tints. */
    final int accent, onAccent, link, pressed, activated;
    /** Navigation hero, the copy button, the All screens icon, and the idle status dot. */
    final int hero, quiet, screensIcon, idle;
    /** Gauge track below and above the warm threshold. */
    final int coldTrack, warmTrack;

    private Palette(boolean dark, int background, int surface, int card, int line, int ink, int muted,
                    int secondary, int accent, int onAccent, int link, int pressed, int activated, int hero,
                    int quiet, int screensIcon, int idle, int coldTrack, int warmTrack) {
        this.dark = dark; this.background = background; this.surface = surface; this.card = card;
        this.line = line; this.ink = ink; this.muted = muted; this.secondary = secondary;
        this.accent = accent; this.onAccent = onAccent; this.link = link;
        this.pressed = pressed; this.activated = activated;
        this.hero = hero; this.quiet = quiet; this.screensIcon = screensIcon; this.idle = idle;
        this.coldTrack = coldTrack; this.warmTrack = warmTrack;
    }

    static final Palette LIGHT = new Palette(false,
        0xffffffff, 0xfff0f2f6, 0xffffffff, 0xffdfe3ea,
        0xff101522, 0xff677388, 0xff45536a,
        0xff007aff, 0xffffffff, 0xff007aff, 0xffe7f0ff, 0xffd9eaff,
        0xffe8f2ff, 0xfff8f9fb, 0xffd5f0ff, 0xffa0a7b2,
        0xffd5e6ff, 0xffffe7bf);
    static final Palette DARK = new Palette(true,
        0xff0b0f17, 0xff161c27, 0xff1f2733, 0xff2a3242,
        0xffe8ecf3, 0xff97a1b3, 0xffc3cad6,
        0xff2678eb, 0xffffffff, 0xff6aaeff, 0xff1d3150, 0xff1a2d4a,
        0xff14243b, 0xff1f2733, 0xff16324a, 0xff6b7486,
        0xff1d3352, 0xff4a3714);

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
