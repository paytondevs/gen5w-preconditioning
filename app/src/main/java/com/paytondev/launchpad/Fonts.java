package com.paytondev.launchpad;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Bundled Google Sans Flex (SIL OFL, assets/fonts). KitKat can't read variable-font axes, so each weight
 * is a static instance cut from the variable font and subset to Latin; the head unit's own fonts vary by firmware.
 */
final class Fonts {
    enum Weight {
        EXTRA_LIGHT("ExtraLight"), LIGHT("Light"), REGULAR("Regular"), MEDIUM("Medium"), SEMI_BOLD("SemiBold"), BOLD("Bold");
        final String file;
        Weight(String file) { this.file = file; }
    }
    private static final Typeface[] loaded = new Typeface[Weight.values().length];

    static synchronized Typeface get(Context context, Weight weight) {
        Typeface face = loaded[weight.ordinal()];
        if (face == null) {
            try { face = Typeface.createFromAsset(context.getAssets(), "fonts/GoogleSansFlex-" + weight.file + ".ttf"); }
            catch (RuntimeException missing) { face = Typeface.create("sans-serif", Typeface.NORMAL); }
            loaded[weight.ordinal()] = face;
        }
        return face;
    }

    private Fonts() { }
}
