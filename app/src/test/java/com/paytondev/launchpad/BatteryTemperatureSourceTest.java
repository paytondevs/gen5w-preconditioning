package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryTemperatureSourceTest {
    @Test public void bothInspectedProfilesReadTheClusterMinimum() {
        assertTrue(PreconditioningProfile.readsClusterBatteryTemperature(PreconditioningProfile.EV6_DEVICE));
        assertTrue(PreconditioningProfile.readsClusterBatteryTemperature(PreconditioningProfile.DEVICE));
    }
    @Test public void otherDevicesHaveNoTemperatureSource() {
        assertFalse(PreconditioningProfile.readsClusterBatteryTemperature("wp_daudioplus_ne_eu"));
        assertFalse(PreconditioningProfile.readsClusterBatteryTemperature(""));
        assertFalse(PreconditioningProfile.readsClusterBatteryTemperature(null));
    }
    @Test public void minimumTemperatureIsIndexOneOfTheBatteryInfoArray() {
        // Navigation.BATTERY_INFO_RX: 0 XEV_SOCDIS, 1 XEV_MIN_BATT_TEMP, ... 7 VCU_DTE_ADD.
        assertEquals(Integer.valueOf(14), BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410, 0}));
        assertNull(BatteryTemperature.decode(new int[]{120, 14, 16620, 0, 108, 0, 410}));
    }
}
