package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class SplitScreenTest {
    @Test public void onlyStatusOneMeansThePanelIsOpen() {
        assertTrue(SplitScreen.on(1));
        assertFalse(SplitScreen.on(0));
        assertFalse(SplitScreen.on(2));
    }
    @Test public void reservesAThirdOfTheScreenForThePanel() {
        assertEquals(640, SplitScreen.insetPx(1920));
        // Wider than the ~29% the panel covers in photos of a NEEV23 US head unit.
        assertTrue(SplitScreen.insetPx(1920) > Math.round(1920 * 0.29f));
    }
}
