package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AppLanguageTest {
    @Test public void preferenceDefaultsAndToggles() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromPreference(null));
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromPreference("unknown"));
        assertEquals(AppLanguage.KOREAN, AppLanguage.fromPreference("KOREAN"));
        assertEquals(AppLanguage.KOREAN, AppLanguage.ENGLISH.next());
        assertEquals(AppLanguage.ENGLISH, AppLanguage.KOREAN.next());
        assertEquals("설정", AppLanguage.KOREAN.choose("Settings", "설정"));
    }
}
