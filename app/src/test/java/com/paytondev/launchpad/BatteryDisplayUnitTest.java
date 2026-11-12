package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryDisplayUnitTest {
    @Test public void conversionsAndToggleAffectLabelsOnly() {
        assertEquals("—", BatteryDisplayUnit.CELSIUS.formatNullable(null));
        assertEquals("9°C", BatteryDisplayUnit.CELSIUS.formatNullable(9));
        assertEquals("48°F", BatteryDisplayUnit.FAHRENHEIT.formatNullable(9));
        assertEquals("48°F", BatteryDisplayUnit.FAHRENHEIT.format(9));
        assertEquals("9°C", BatteryDisplayUnit.CELSIUS.format(9));
        assertEquals("-10°C", BatteryDisplayUnit.CELSIUS.format(-10));
        assertEquals("14°F", BatteryDisplayUnit.FAHRENHEIT.format(-10));
        assertEquals("21°C", BatteryDisplayUnit.CELSIUS.format(21));
        assertEquals("70°F", BatteryDisplayUnit.FAHRENHEIT.format(21));
    }
    @Test public void currentWarmDetailUsesOnlyTheSelectedUnit() {
        String detail = "Battery minimum 22°C is at or above the expected heating threshold (21°C / 70°F). No request sent.";
        assertEquals("Battery minimum 22°C is at or above the expected heating threshold (21°C). No request sent.",
            BatteryDisplayUnit.CELSIUS.displayCurrentDetail(detail));
        assertEquals("Battery minimum 72°F is at or above the expected heating threshold (70°F). No request sent.",
            BatteryDisplayUnit.FAHRENHEIT.displayCurrentDetail(detail));
        assertEquals(detail, "Battery minimum 22°C is at or above the expected heating threshold (21°C / 70°F). No request sent.");
        assertEquals("Unrelated 5°F", BatteryDisplayUnit.CELSIUS.displayCurrentDetail("Unrelated 5°F"));
    }
}
