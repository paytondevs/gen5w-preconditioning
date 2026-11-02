package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryTemperatureSourceTest {
    @Test public void minimumTemperatureIsIndexOneOfTheBatteryInfoArray() {
        // Navigation.BATTERY_INFO_RX: 0 XEV_SOCDIS, 1 XEV_MIN_BATT_TEMP, ... 7 VCU_DTE_ADD.
        assertEquals(Integer.valueOf(14), BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410, 0}));
        assertNull(BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410}));
    }
    @Test public void outsideAirIsIndexFourAtHalfDegreesFromMinusForty() {
        // DATC_OUT_TEMP_SNSR; scaling unverified against the car's own display.
        assertEquals(Integer.valueOf(14), BatteryTemperature.decodeOutside(new int[]{120, 14, 16620, 0, 108, 0, 410, 0}));
        assertEquals(Integer.valueOf(17), BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 113, 0, 202, 0})); // 16.5 rounds up
        assertEquals(Integer.valueOf(-10), BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 60, 0, 202, 0}));
        assertNull(BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 0, 0, 202, 0}));      // no reading
        assertNull(BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 255, 0, 202, 0}));    // reserved
        assertNull(BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 250, 0, 202, 0}));    // 85 °C: implausible
        assertNull(BatteryTemperature.decodeOutside(new int[]{96, 99, 16620, 0, 108, 0, 202, 0}));    // battery fields invalid
        assertNull(BatteryTemperature.decodeOutside(new int[]{96, 20, 16620, 0, 108, 0, 202}));
        assertNull(BatteryTemperature.decodeOutside(null));
    }
}
