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
    @Test public void lightPaletteKeepsTheOriginalColors() {
        assertEquals(0xffffffff, Palette.LIGHT.background);
        assertEquals(0xfff0f2f6, Palette.LIGHT.surface);
        assertEquals(0xff101522, Palette.LIGHT.ink);
        assertEquals(0xff677388, Palette.LIGHT.muted);
        assertEquals(0xffdfe3ea, Palette.LIGHT.line);
        assertEquals(TemperatureGauge.COLD, Palette.LIGHT.accent);
        assertSame(Palette.DARK, Palette.of(true));
    }
    @Test public void darkTextIsReadableOnEveryDarkBackground() {
        Palette d = Palette.DARK;
        for (int background : new int[]{d.background, d.surface, d.card, d.hero}) {
            assertTrue(Palette.contrast(d.ink, background) >= 7);
            assertTrue(Palette.contrast(d.muted, background) >= 4.5);
            assertTrue(Palette.contrast(d.secondary, background) >= 4.5);
        }
        assertTrue(Palette.contrast(d.link, d.card) >= 4.5);
        assertTrue(Palette.contrast(d.link, d.pressed) >= 4.5);
        // White on the accent is at least as readable as in the light palette.
        assertTrue(Palette.contrast(d.onAccent, d.accent) >= Palette.contrast(Palette.LIGHT.onAccent, Palette.LIGHT.accent));
    }
}
