package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationPolicyTest {
    @Test public void backLeavesSubPagesForSettingsThenBatteryThenCloses() {
        assertNull(ScreenFlow.back(ScreenFlow.Page.BATTERY));
        assertEquals(ScreenFlow.Page.BATTERY, ScreenFlow.back(ScreenFlow.Page.SCHEDULE));
        assertEquals(ScreenFlow.Page.BATTERY, ScreenFlow.back(ScreenFlow.Page.SETTINGS));
        for (ScreenFlow.Page sub : new ScreenFlow.Page[]{ScreenFlow.Page.WIFI, ScreenFlow.Page.SIGNALS, ScreenFlow.Page.BROWSER})
            assertEquals(ScreenFlow.Page.SETTINGS, ScreenFlow.back(sub));
    }
    @Test public void subPagesKeepSettingsSelectedInTheMenu() {
        for (ScreenFlow.Page page : ScreenFlow.MENU) assertEquals(page, ScreenFlow.menu(page));
        assertEquals(ScreenFlow.Page.SETTINGS, ScreenFlow.menu(ScreenFlow.Page.WIFI));
        assertEquals(ScreenFlow.Page.SETTINGS, ScreenFlow.menu(ScreenFlow.Page.SIGNALS));
        assertEquals(ScreenFlow.Page.SETTINGS, ScreenFlow.menu(ScreenFlow.Page.BROWSER));
    }
    @Test public void knobWrapsAndSkipsUnavailableControls() {
        boolean[] enabled = {true, false, true, false};
        assertEquals(0, KnobFocus.next(-1, 1, enabled));
        assertEquals(2, KnobFocus.next(0, 1, enabled));
        assertEquals(0, KnobFocus.next(2, 1, enabled));
        assertEquals(2, KnobFocus.next(0, -1, enabled));
        assertEquals(2, KnobFocus.next(-1, -1, enabled));
        assertEquals(-1, KnobFocus.next(-1, 1, new boolean[]{false, false}));
        assertEquals(-1, KnobFocus.next(-1, 1, new boolean[]{}));
    }
    @Test public void missingDisabledPrivateAndProtectedActivitiesCannotLaunch() {
        assertTrue(ShortcutSpec.mayLaunch(true, true, true, true, true));
        assertFalse(ShortcutSpec.mayLaunch(false, true, true, true, true));
        assertFalse(ShortcutSpec.mayLaunch(true, false, true, true, true));
        assertFalse(ShortcutSpec.mayLaunch(true, true, false, true, true));
        assertFalse(ShortcutSpec.mayLaunch(true, true, true, false, true));
        assertFalse(ShortcutSpec.mayLaunch(true, true, true, true, false));
    }
}
