package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppearanceTest {
    @Test public void followCarMirrorsTheCarAndFixedModesIgnoreIt() {
        assertFalse(AppearanceMode.FOLLOW_CAR.dark(false));
        assertTrue(AppearanceMode.FOLLOW_CAR.dark(true));
        assertFalse(AppearanceMode.LIGHT.dark(true));
        assertTrue(AppearanceMode.DARK.dark(false));
    }
    @Test public void unknownPreferenceFollowsTheCar() {
        assertEquals(AppearanceMode.FOLLOW_CAR, AppearanceMode.fromPreference(null));
        assertEquals(AppearanceMode.FOLLOW_CAR, AppearanceMode.fromPreference("SEPIA"));
        assertEquals(AppearanceMode.DARK, AppearanceMode.fromPreference("DARK"));
    }
    @Test public void textIsReadableOnEveryBackgroundInBothPalettes() {
        assertSame(Palette.DARK, Palette.of(true));
        assertSame(Palette.LIGHT, Palette.of(false));
        for (Palette p : new Palette[]{Palette.DARK, Palette.LIGHT}) {
            for (int background : new int[]{p.background, p.surface, p.raised, p.focus, p.accentSoft}) {
                assertTrue(Palette.contrast(p.ink, background) >= 7);
                assertTrue(Palette.contrast(p.muted, background) >= 4.5);
            }
            assertTrue(Palette.contrast(p.onAccent, p.accent) >= 4.5);
            assertTrue(Palette.contrast(p.onAccent, p.accentFocus) >= 3);
            assertTrue(Palette.contrast(p.accent, p.accentSoft) >= 4.5);
            assertTrue(Palette.contrast(p.warm, p.warmSoft) >= 4.5);
            assertTrue(Palette.contrast(p.warm, p.raised) >= 4.5);
        }
    }
    @Test public void knobSelectionChangesTheFillVisibly() {
        for (Palette p : new Palette[]{Palette.DARK, Palette.LIGHT}) {
            assertTrue(Palette.contrast(p.focus, p.raised) >= 1.3);
            assertTrue(Palette.contrast(p.focus, p.accentSoft) >= 1.2);
            assertTrue(Palette.contrast(p.accentFocus, p.accent) >= 1.3);
        }
    }
}
