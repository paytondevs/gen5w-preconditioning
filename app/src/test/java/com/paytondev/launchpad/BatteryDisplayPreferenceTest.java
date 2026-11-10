package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class BatteryDisplayPreferenceTest {
    @Test public void automaticUsesReportedMarketAndFallsBackWithoutGuessing() {
        assertEquals(BatteryDisplayUnit.CELSIUS, BatteryDisplayPreference.AUTO.resolve("NEEV.KOR.0000.V039.001.260902"));
        assertEquals(BatteryDisplayUnit.CELSIUS, BatteryDisplayPreference.AUTO.resolve("CVEV.KOR.0000.V039.001.260902"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("NEEV23.USA.0000.V016.010.260903"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve(null));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve(""));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("NEEV.EUR.0000.V000"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("NEEV.XKOR.0000"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("NEEV.KOR."));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("!.KOR.0000"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.AUTO.resolve("KOR"));
    }
    @Test public void savedExplicitChoiceAlwaysOverridesTheReportedMarket() {
        assertEquals(BatteryDisplayUnit.CELSIUS, BatteryDisplayPreference.fromPreference("CELSIUS").resolve("NEEV.USA.0000"));
        assertEquals(BatteryDisplayUnit.FAHRENHEIT, BatteryDisplayPreference.fromPreference("FAHRENHEIT").resolve("NEEV.KOR.0000"));
        assertEquals(BatteryDisplayPreference.AUTO, BatteryDisplayPreference.fromPreference(null));
        assertEquals(BatteryDisplayPreference.AUTO, BatteryDisplayPreference.fromPreference("bad value"));
        assertEquals(BatteryDisplayPreference.AUTO, BatteryDisplayPreference.fromPreference("AUTO"));
    }
    @Test public void choiceCyclesAndShowsTheResolvedAutoUnit() {
        assertEquals(BatteryDisplayPreference.CELSIUS, BatteryDisplayPreference.AUTO.next());
        assertEquals(BatteryDisplayPreference.FAHRENHEIT, BatteryDisplayPreference.CELSIUS.next());
        assertEquals(BatteryDisplayPreference.AUTO, BatteryDisplayPreference.FAHRENHEIT.next());
        assertEquals("Auto (°C)", BatteryDisplayPreference.AUTO.title(BatteryDisplayUnit.CELSIUS));
        assertEquals("°F", BatteryDisplayPreference.FAHRENHEIT.title(BatteryDisplayUnit.FAHRENHEIT));
    }
}
