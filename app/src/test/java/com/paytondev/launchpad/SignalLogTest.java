package com.paytondev.launchpad;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class SignalLogTest {
    @Test public void parsesGetpropLines() {
        Map<String, String> values = new HashMap<>();
        SignalLog.parseProp("[persist.sys.daynight]: [1]", values);
        SignalLog.parseProp("[ro.build.product]: []", values);
        SignalLog.parseProp("not a property", values);
        assertEquals("1", values.get("prop/persist.sys.daynight"));
        assertEquals("", values.get("prop/ro.build.product"));
        assertEquals(2, values.size());
    }
    @Test public void keywordsFindLikelyDayNightNames() {
        assertTrue(SignalLog.interesting("system/hkmc_daynight_mode"));
        assertTrue(SignalLog.interesting("prop/persist.sys.ILLUMINATION"));
        assertFalse(SignalLog.interesting("system/screen_off_timeout"));
    }
    @Test public void recordsAnyChangeAndForgetsReverts() {
        Map<String, String> first = new HashMap<>();
        first.put("uimode/night", "no"); first.put("system/x", "0"); first.put("system/display_dark", "0");
        SignalLog log = new SignalLog(first);
        Map<String, String> night = new HashMap<>(first);
        night.put("system/x", "1"); night.put("global/new_key", "on");
        log.update(night);
        assertArrayEquals(new String[]{"0", "1"}, log.changes().get("system/x"));
        assertArrayEquals(new String[]{null, "on"}, log.changes().get("global/new_key"));
        List<String> lines = log.lines();
        assertEquals("uimode/night = no", lines.get(0));
        assertTrue(lines.contains("system/x: 0 → 1"));
        assertTrue(lines.contains("global/new_key: (none) → on"));
        assertTrue(lines.contains("system/display_dark = 0"));
        log.update(first);
        assertTrue(log.changes().isEmpty());
        assertTrue(log.lines().contains("No changes yet. Switch the Hyundai display between Day and Night."));
    }
}
