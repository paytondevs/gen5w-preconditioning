package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationPolicyTest {
    @Test public void backReturnsThroughTheExistingActivityToTheDefaultTab() {
        ScreenFlow.Page precondition = ScreenFlow.Page.PRECONDITIONING, home = ScreenFlow.Page.HOME;
        ScreenFlow.Page advanced = ScreenFlow.Page.ADVANCED, browser = ScreenFlow.Page.BROWSER;
        for (ScreenFlow.Page defaultPage : ScreenFlow.TABS) {
            assertEquals(advanced, ScreenFlow.back(browser, defaultPage));
            assertNull(ScreenFlow.back(defaultPage, defaultPage));
            for (ScreenFlow.Page tab : ScreenFlow.TABS)
                if (tab != defaultPage) assertEquals(defaultPage, ScreenFlow.back(tab, defaultPage));
        }
        assertEquals(precondition, ScreenFlow.back(home, precondition));
        assertEquals(home, ScreenFlow.back(advanced, home));
        assertNull(ScreenFlow.back(advanced, advanced));
    }
    @Test public void defaultTabPreferenceFallsBackToPreconditioning() {
        assertEquals(ScreenFlow.Page.PRECONDITIONING, ScreenFlow.fromPreference(null));
        assertEquals(ScreenFlow.Page.PRECONDITIONING, ScreenFlow.fromPreference(""));
        assertEquals(ScreenFlow.Page.PRECONDITIONING, ScreenFlow.fromPreference("home"));
        assertEquals(ScreenFlow.Page.PRECONDITIONING, ScreenFlow.fromPreference("BROWSER"));
        assertEquals(ScreenFlow.Page.HOME, ScreenFlow.fromPreference("HOME"));
        assertEquals(ScreenFlow.Page.ADVANCED, ScreenFlow.fromPreference("ADVANCED"));
        for (ScreenFlow.Page tab : ScreenFlow.TABS) assertEquals(tab, ScreenFlow.fromPreference(tab.name()));
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
