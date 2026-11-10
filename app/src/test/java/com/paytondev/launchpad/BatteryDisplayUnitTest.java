package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryDisplayUnitTest {
    @Test public void conversionsAndToggleAffectLabelsOnly() {
        assertEquals("48°F", BatteryDisplayUnit.FAHRENHEIT.format(9));
        assertEquals("9°C", BatteryDisplayUnit.CELSIUS.format(9));
        assertEquals("-10°C", BatteryDisplayUnit.CELSIUS.format(-10));
        assertEquals("14°F", BatteryDisplayUnit.FAHRENHEIT.format(-10));
        assertEquals("21°C", BatteryDisplayUnit.CELSIUS.format(21));
        assertEquals("70°F", BatteryDisplayUnit.FAHRENHEIT.format(21));
        assertEquals(BatteryDisplayUnit.CELSIUS, BatteryDisplayUnit.FAHRENHEIT.other());
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayUnit.CELSIUS.other());
    }
}
