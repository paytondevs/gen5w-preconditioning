package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryTemperatureSourceTest {
    @Test public void minimumTemperatureIsIndexOneOfTheBatteryInfoArray() {
        // Navigation.BATTERY_INFO_RX: 0 XEV_SOCDIS, 1 XEV_MIN_BATT_TEMP, ... 7 VCU_DTE_ADD.
        assertEquals(Integer.valueOf(14), BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410, 0}));
        assertNull(BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410}));
    }
}
