package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class WarmBatteryTest {
    private PreconditioningController ready(PreconditioningControllerTest.FakeVehicle car,
                                            PreconditioningControllerTest.Journal journal) {
        PreconditioningController c = new PreconditioningController(car, journal);
        c.refresh(1000);
        return c;
    }
    @Test public void thresholdSkipsAllCommandsAndResetsAfterTenSeconds() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningControllerTest.Journal journal = new PreconditioningControllerTest.Journal();
        PreconditioningController c = ready(car, journal);
        assertFalse(c.start(1000, 20));
        assertEquals(PreconditioningSnapshot.State.WARM, c.snapshot(1000).state);
        assertEquals(10, c.snapshot(1000).resetSeconds);
        assertTrue(c.snapshot(1000).detail.contains("No request sent"));
        assertFalse(c.snapshot(1000).requested);
        assertFalse(c.snapshot(1000).canStop);
        assertFalse(journal.pending);
        c.refresh(10000);
        assertEquals(1, c.snapshot(10000).resetSeconds);
        c.refresh(11000);
        assertEquals(PreconditioningSnapshot.State.IDLE, c.snapshot(11000).state);
        assertEquals(0, c.snapshot(11000).resetSeconds);
        assertTrue(c.snapshot(11000).canStart);
        assertEquals(0, car.starts + car.stops + car.maintenance);
        assertFalse(c.snapshot(11000).detail.contains("resets"));
    }
    @Test public void coolerAndUnknownTemperaturesUseExistingRequestPath() {
        for (Integer temperature : new Integer[]{17, 19, -10, null, 127, 255}) {
            PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
            PreconditioningControllerTest.Journal journal = new PreconditioningControllerTest.Journal();
            PreconditioningController c = ready(car, journal);
            assertTrue(c.start(1000, temperature));
            assertEquals(1, car.starts);
            assertTrue(journal.pending);
            assertEquals(PreconditioningSnapshot.State.WAITING, c.snapshot(1000).state);
        }
    }
    @Test public void duplicateStartDoesNotBypassAdvisoryButExplicitOverrideDoes() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningControllerTest.Journal journal = new PreconditioningControllerTest.Journal();
        PreconditioningController c = ready(car, journal);
        c.start(1000, 23);
        assertFalse(c.start(1001, null));
        assertEquals(0, car.starts);
        assertTrue(c.startAnyway(2000));
        assertEquals(1, car.starts);
        assertTrue(journal.pending);
        assertEquals(0, c.snapshot(2000).resetSeconds);
        assertFalse(c.startAnyway(2001));
        c.refresh(12000);
        assertTrue(c.snapshot(12000).requested);
        assertEquals(0, car.stops); // Notice expiry must not cancel a real request.
    }
    @Test public void expiredOverrideCannotSendAndNextNormalPressChecksTemperatureAgain() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningController c = ready(car, new PreconditioningControllerTest.Journal());
        c.start(1000, 20);
        c.refresh(11000);
        assertFalse(c.startAnyway(11000));
        assertFalse(c.start(12000, 21));
        assertEquals(10, c.snapshot(12000).resetSeconds);
        assertEquals(0, car.starts);
    }
    @Test public void ioniq5ThresholdStartsAt21AndNamesSeventyFahrenheit() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        car.warmThreshold = PreconditioningProfile.IONIQ5_WARM_THRESHOLD_C;
        PreconditioningController c = ready(car, new PreconditioningControllerTest.Journal());
        assertTrue(c.start(1000, 20));
        PreconditioningControllerTest.FakeVehicle warm = new PreconditioningControllerTest.FakeVehicle();
        warm.warmThreshold = PreconditioningProfile.IONIQ5_WARM_THRESHOLD_C;
        PreconditioningController d = ready(warm, new PreconditioningControllerTest.Journal());
        assertFalse(d.start(1000, 21));
        assertEquals(PreconditioningSnapshot.State.WARM, d.snapshot(1000).state);
        assertTrue(d.snapshot(1000).detail.contains("(21°C / 70°F)"));
        assertEquals(0, warm.starts);
    }
    @Test public void coolingAfterNoticeAllowsNormalStart() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningController c = ready(car, new PreconditioningControllerTest.Journal());
        c.start(1000, 20); c.refresh(11000);
        assertTrue(c.start(12000, 19));
        assertEquals(1, car.starts);
    }
    @Test public void lostReadyOrCarControlledActivationCannotBeOverridden() {
        for (VehiclePort.Reading changed : new VehiclePort.Reading[]{
                new VehiclePort.Reading(2, 0, 0, -1), new VehiclePort.Reading(2, 1, 1, -1),
                new VehiclePort.Reading(1, 0, 1, -1)}) {
            PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
            PreconditioningController c = ready(car, new PreconditioningControllerTest.Journal());
            c.start(1000, 20); car.reading = changed; c.refresh(2000);
            assertFalse(c.startAnyway(2000));
            assertFalse(c.snapshot(2000).canStart);
            assertEquals(0, car.starts + car.stops);
        }
    }
    @Test public void disconnectedStatusDoesNotLeaveWarmOverrideArmedAfterDeadline() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningController c = ready(car, new PreconditioningControllerTest.Journal());
        c.start(1000, 20); car.readFails = true; c.refresh(11000);
        assertEquals(PreconditioningSnapshot.State.UNKNOWN, c.snapshot(11000).state);
        assertEquals(0, c.snapshot(11000).resetSeconds);
        assertFalse(c.startAnyway(11000));
        car.readFails = false; c.refresh(12000);
        assertTrue(c.snapshot(12000).canStart);
    }
    @Test public void recreatedControllerDoesNotPersistAWarmNoticeOrSendCommands() {
        PreconditioningControllerTest.FakeVehicle car = new PreconditioningControllerTest.FakeVehicle();
        PreconditioningControllerTest.Journal journal = new PreconditioningControllerTest.Journal();
        PreconditioningController old = ready(car, journal); old.start(1000, 22);
        PreconditioningController fresh = new PreconditioningController(car, journal);
        fresh.recover(journal.pending, 2000); fresh.refresh(2000);
        assertEquals(PreconditioningSnapshot.State.IDLE, fresh.snapshot(2000).state);
        assertEquals(0, car.starts + car.stops);
    }
    @Test public void nativeArrayDecoderRejectsMalformedAndReservedReadings() {
        assertEquals(Integer.valueOf(20), BatteryTemperature.decode(new int[]{96,20,16620,0,113,0,202,0}));
        assertEquals(Integer.valueOf(17), BatteryTemperature.decode(new int[]{100,17,0,0,0,0,0,0}));
        assertNull(BatteryTemperature.decode(null));
        assertNull(BatteryTemperature.decode(new int[7]));
        for (int temperature : new int[]{-128, -41, 61, 127, 255, 65535})
            assertNull(BatteryTemperature.decode(new int[]{96,temperature,0,0,0,0,0,0}));
        assertNull(BatteryTemperature.decode(new int[]{255,20,0,0,0,0,0,0}));
    }
}
