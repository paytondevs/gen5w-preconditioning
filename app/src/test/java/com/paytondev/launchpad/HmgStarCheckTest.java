package com.paytondev.launchpad;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class HmgStarCheckTest {
    private Map<String,Object> expected() {
        Map<String,Object> e = new LinkedHashMap<>();
        for (int i = 0; i < 31; i++) e.put("buttonsettings.p" + i, "shape" + i);
        return e;
    }
    private Map<String,Object> points() {
        Map<String,Object> p = expected();
        p.put("framework.resources.get_text", "text");
        p.put("framework.resources.get_drawable", "drawable");
        return p;
    }
    @Test public void requiresTheCheckedExpectedMapAndFrameworkResources() {
        Map<String,Object> expected = expected(), points = points();
        assertTrue(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
        assertFalse(HmgStarCheck.supports(false, true, "hyundai/neev/kor/260902", points, expected));
        assertFalse(HmgStarCheck.supports(true, false, "hyundai/neev/kor/260902", points, expected));
        assertFalse(HmgStarCheck.supports(true, true, "other", points, expected));
        points.remove("framework.resources.get_drawable");
        assertFalse(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
    }
    @Test public void missingOrDifferentStarPointIsRejected() {
        Map<String,Object> expected = expected(), points = points();
        points.remove("buttonsettings.p0");
        assertFalse(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
        points.put("buttonsettings.p0", "wrong");
        assertFalse(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
    }
    @Test public void rejectsUnexpectedCountOrScope() {
        Map<String,Object> expected = expected(), points = points();
        expected.remove("buttonsettings.p0");
        assertFalse(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
        expected.put("launcher.unexpected", "other"); points.put("launcher.unexpected", "other");
        assertFalse(HmgStarCheck.supports(true, true, "hyundai/neev/kor/260902", points, expected));
    }
}
