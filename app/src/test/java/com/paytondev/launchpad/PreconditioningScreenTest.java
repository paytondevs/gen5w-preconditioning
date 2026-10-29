package com.paytondev.launchpad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreconditioningScreenTest {
    private static List<TemperatureHistory.Sample> samples(long... times) {
        List<TemperatureHistory.Sample> list = new ArrayList<>();
        for (long at : times) list.add(new TemperatureHistory.Sample(at, 10, null));
        return list;
    }
    @Test public void gaugeClampsToItsRangeAndMarksTheThreshold() {
        assertEquals(0f, TemperatureGauge.fraction(-40), 0f);
        assertEquals(0f, TemperatureGauge.fraction(-10), 0f);
        assertEquals(30f / 45f, TemperatureGauge.fraction(20), 1e-6f);
        assertEquals(1f, TemperatureGauge.fraction(35), 0f);
        assertEquals(1f, TemperatureGauge.fraction(60), 0f);
        assertEquals(TemperatureGauge.COLD, TemperatureGauge.color(19, 20));
        assertEquals(TemperatureGauge.WARM, TemperatureGauge.color(20, 20));
        assertEquals(TemperatureGauge.COLD, TemperatureGauge.color(20, 21));
        assertEquals(TemperatureGauge.COLD, TemperatureGauge.color(null, 20));
    }
    @Test public void chartRangeAlwaysIncludesTheThresholdAndTenDegrees() {
        assertArrayEquals(new float[]{15, 25}, TemperatureChart.range(Collections.<TemperatureHistory.Sample>emptyList(), 20), 0f);
        assertArrayEquals(new float[]{16, 26}, TemperatureChart.range(Collections.<TemperatureHistory.Sample>emptyList(), 21), 0f);
        assertArrayEquals(new float[]{-7, 22}, TemperatureChart.range(Arrays.asList(
            new TemperatureHistory.Sample(1, -5, null), new TemperatureHistory.Sample(2, 3, 6)), 20), 0f);
        assertArrayEquals(new float[]{18, 33}, TemperatureChart.range(Arrays.asList(
            new TemperatureHistory.Sample(1, 22, 31)), 20), 0f);
        float[] narrow = TemperatureChart.range(Arrays.asList(new TemperatureHistory.Sample(1, 19, 21)), 20);
        assertEquals(10f, narrow[1] - narrow[0], 0f); assertTrue(narrow[0] <= 19 && narrow[1] >= 21);
    }
    @Test public void chartSpanIsBetweenFiveAndThirtyMinutes() {
        long now = 10_000_000;
        assertEquals(now - TemperatureChart.MIN_SPAN_MS, TemperatureChart.start(samples(), now));
        assertEquals(now - TemperatureChart.MIN_SPAN_MS, TemperatureChart.start(samples(now - 60_000), now));
        assertEquals(now - 600_000, TemperatureChart.start(samples(now - 600_000, now - 1000), now));
        assertEquals(now - TemperatureHistory.WINDOW_MS, TemperatureChart.start(samples(now - 4_000_000), now));
    }
    @Test public void chartMappingIsLinearAndClamped() {
        assertEquals(100f, TemperatureChart.x(0, 0, 1000, 100, 500), 0f);
        assertEquals(350f, TemperatureChart.x(500, 0, 1000, 100, 500), 0f);
        assertEquals(600f, TemperatureChart.x(2000, 0, 1000, 100, 500), 0f);
        assertEquals(100f, TemperatureChart.x(-5, 0, 1000, 100, 500), 0f);
        assertEquals(600f, TemperatureChart.x(5, 5, 5, 100, 500), 0f);
        assertEquals(210f, TemperatureChart.y(10, 10, 20, 10, 200), 0f);
        assertEquals(10f, TemperatureChart.y(20, 10, 20, 10, 200), 0f);
        assertEquals(110f, TemperatureChart.y(15, 10, 20, 10, 200), 0f);
        assertEquals(10f, TemperatureChart.y(99, 10, 20, 10, 200), 0f);
    }
    @Test public void fahrenheitRoundsLikeTheExistingLabels() {
        assertEquals(68, BatteryTemperatureReading.fahrenheit(20));
        assertEquals(14, BatteryTemperatureReading.fahrenheit(-10));
        assertEquals(95, BatteryTemperatureReading.fahrenheit(35));
        assertEquals(-40, BatteryTemperatureReading.fahrenheit(-40));
    }
    @Test public void temperatureAgeSeparatesValueFromReceipt() {
        assertEquals("Checking…", BatteryTemperatureReading.checking().age(0));
        assertEquals("Unavailable", BatteryTemperatureReading.checking().update(null, 1000).age(1000));
        BatteryTemperatureReading reading = BatteryTemperatureReading.checking().update(17, 1000);
        assertEquals("Received 9s ago", reading.age(10_000));
        assertEquals("Stale · last received 30s ago", reading.age(31_000));
    }
    @Test public void vehicleChecksReadRawValuesAndNeverGuess() {
        PreconditioningSnapshot unknown = PreconditioningSnapshot.checking();
        assertEquals(PreconditioningSnapshot.UNKNOWN, unknown.readyCheck());
        assertEquals(PreconditioningSnapshot.UNKNOWN, unknown.enabledCheck());
        assertEquals(PreconditioningSnapshot.UNKNOWN, unknown.indicatorCheck());
        assertEquals("Checking", unknown.supportedValue());
        PreconditioningSnapshot ready = snapshot(1, 2, 0);
        assertEquals(PreconditioningSnapshot.PASS, ready.readyCheck());
        assertEquals(PreconditioningSnapshot.PASS, ready.enabledCheck());
        assertEquals(PreconditioningSnapshot.PASS, ready.indicatorCheck());
        assertEquals("Off", ready.indicatorValue());
        PreconditioningSnapshot off = snapshot(0, 1, 7);
        assertEquals(PreconditioningSnapshot.FAIL, off.readyCheck());
        assertEquals(PreconditioningSnapshot.FAIL, off.enabledCheck());
        assertEquals(PreconditioningSnapshot.UNKNOWN, off.indicatorCheck());
        assertEquals("Off in EV settings (raw 1)", off.enabledValue());
        assertEquals("Unknown (raw 7)", off.indicatorValue());
        assertEquals("Unknown (raw 3)", snapshot(3, 2, 1).readyValue());
    }
    @Test public void controllerSnapshotCarriesReadingAndFirmwareChecks() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningController controller = new PreconditioningController(car, new PreconditioningControllerTest.Journal());
        PreconditioningSnapshot before = controller.snapshot(0);
        assertEquals(-1, before.ready); assertEquals(-1, before.enabled);
        assertEquals(PreconditioningSnapshot.UNKNOWN, before.supported);
        controller.refresh(1000);
        PreconditioningSnapshot read = controller.snapshot(1000);
        assertEquals(1, read.ready); assertEquals(2, read.enabled); assertEquals(PreconditioningSnapshot.PASS, read.supported);
        car.readFails = true; controller.refresh(2000);
        assertEquals(-1, controller.snapshot(2000).ready);
        assertEquals(PreconditioningSnapshot.UNKNOWN, controller.snapshot(2000).supported);
        car.readFails = false; car.unavailable = "Unsupported head-unit firmware"; controller.refresh(3000);
        assertEquals(PreconditioningSnapshot.FAIL, controller.snapshot(3000).supported);
    }
    private static PreconditioningSnapshot snapshot(int ready, int enabled, int indicator) {
        return new PreconditioningSnapshot(PreconditioningSnapshot.State.IDLE, false, false, false, indicator, -1, 0, "",
            0, ready, enabled, PreconditioningSnapshot.PASS);
    }
}
