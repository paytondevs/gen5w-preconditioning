package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreconditioningControllerTest {
    static final class FakeVehicle implements VehiclePort {
        Reading reading = new Reading(2, 0, 1, -1);
        String unavailable = "";
        int starts, stops, maintenance;
        boolean readFails, startFails, stopFails;
        long interval, timeout = PreconditioningController.ACTIVATION_TIMEOUT_MS;
        @Override public long activationTimeoutMs() { return timeout; }
        @Override public String unavailableReason() { return unavailable; }
        @Override public Reading read() throws Exception {
            if (readFails) throw new Exception("Disconnected");
            return reading;
        }
        @Override public void beginManual() throws Exception { starts++; if (startFails) throw new Exception("Rejected"); }
        @Override public void maintainManual() { maintenance++; }
        @Override public void stopAndRelease() throws Exception { stops++; if (stopFails) throw new Exception("Rejected"); }
        @Override public long maintenanceIntervalMs() { return interval; }
    }
    static final class Journal implements PreconditioningController.Journal {
        boolean pending;
        @Override public void requested(boolean value) { pending = value; }
    }
    private static PreconditioningController ready(FakeVehicle car, Journal journal) {
        PreconditioningController controller = new PreconditioningController(car, journal);
        controller.refresh(1000); return controller;
    }
    @Test public void waitingBeforeDeadlineDoesNotRestartOrClaimActive() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); assertTrue(c.start(1000));
        for (long now : new long[]{61000, 121000, 300999}) {
            c.refresh(now);
            assertEquals(PreconditioningSnapshot.State.WAITING, c.snapshot(now).state);
            assertTrue(c.snapshot(now).canStop); assertTrue(journal.pending);
        }
        assertEquals(1, car.starts); assertEquals(0, car.maintenance);
        assertTrue(c.snapshot(300999).detail.contains("Cancels after 5 minutes"));
    }
    @Test public void ioniqTwoMinuteTimeoutCancelsAtItsOwnDeadline() {
        FakeVehicle car = new FakeVehicle(); car.interval = 5000;
        car.timeout = PreconditioningProfile.IONIQ5_ACTIVATION_TIMEOUT_MS; Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); assertTrue(c.start(1000));
        c.refresh(120999);
        assertEquals(PreconditioningSnapshot.State.WAITING, c.snapshot(120999).state);
        assertEquals(0, car.stops);
        assertTrue(c.snapshot(120999).detail.contains("Cancels after 2 minutes"));
        c.refresh(121000);
        assertEquals(1, car.stops); assertTrue(journal.pending);
        assertTrue(c.snapshot(121000).detail.contains("within 2 minutes"));
        c.refresh(122000);
        assertEquals(PreconditioningSnapshot.State.TIMED_OUT, c.snapshot(122000).state);
        assertFalse(journal.pending);
    }
    @Test public void ioniqActivationBeforeTwoMinutesIsNotCancelled() {
        FakeVehicle car = new FakeVehicle(); car.interval = 5000;
        car.timeout = PreconditioningProfile.IONIQ5_ACTIVATION_TIMEOUT_MS; Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); assertTrue(c.start(1000));
        c.refresh(66000); car.reading = new VehiclePort.Reading(2, 1, 1, -1); c.refresh(71000);
        c.refresh(600000);
        assertEquals(PreconditioningSnapshot.State.ACTIVE, c.snapshot(600000).state);
        assertEquals(0, car.stops);
    }
    @Test public void eachProfileHasItsOwnActivationTimeout() {
        assertEquals(5 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs(PreconditioningProfile.EV6_DEVICE));
        assertEquals(2 * 60 * 1000L, PreconditioningProfile.activationTimeoutMs(PreconditioningProfile.DEVICE));
    }
    @Test public void timeoutCancelsOnceAndRestoresStartOnlyAfterFreshOff() {
        FakeVehicle car = new FakeVehicle(); car.interval = 5000; Journal journal = new Journal();
        PreconditioningController c = ready(car,journal); c.start(1000);
        c.refresh(301000);
        assertEquals(1,car.stops); assertEquals(0,car.maintenance); assertTrue(journal.pending);
        assertEquals(PreconditioningSnapshot.State.STOPPING,c.snapshot(301000).state);
        assertFalse(c.snapshot(301000).canStart);
        c.refresh(302000);
        assertEquals(PreconditioningSnapshot.State.TIMED_OUT,c.snapshot(302000).state);
        assertTrue(c.snapshot(302000).canStart); assertFalse(journal.pending);
        assertEquals("No activation",c.snapshot(302000).title());
        c.refresh(999000); assertEquals(1,car.stops); assertEquals(1,car.starts);
        assertEquals(302000-1000,c.snapshot(999000).elapsedMs);
    }
    @Test public void activeAtDeadlineOrBeforeIsNotTimedOut() {
        for(long activatedAt:new long[]{300999,301000}) {
            FakeVehicle car=new FakeVehicle(); PreconditioningController c=ready(car,new Journal());
            c.start(1000); car.reading=new VehiclePort.Reading(2,1,1,-1); c.refresh(activatedAt);
            c.refresh(1201000);
            assertEquals(PreconditioningSnapshot.State.ACTIVE,c.snapshot(1201000).state);
            assertEquals(0,car.stops);
        }
    }
    @Test public void timeoutStillCancelsWhenReadsFailButDoesNotClaimOff() {
        FakeVehicle car=new FakeVehicle(); Journal journal=new Journal();
        PreconditioningController c=ready(car,journal); c.start(1000); car.readFails=true;
        c.refresh(301000); c.refresh(302000);
        assertEquals(1,car.stops); assertTrue(journal.pending); assertFalse(c.snapshot(302000).canStart);
        car.readFails=false; c.refresh(303000);
        assertEquals(PreconditioningSnapshot.State.TIMED_OUT,c.snapshot(303000).state);
        assertFalse(journal.pending);
    }
    @Test public void timeoutStillCancelsWithReservedIndicator() {
        FakeVehicle car=new FakeVehicle(); Journal journal=new Journal();
        PreconditioningController c=ready(car,journal); c.start(1000);
        car.reading=new VehiclePort.Reading(2,2,1,-1); c.refresh(301000); c.refresh(302000);
        assertEquals(1,car.stops); assertTrue(journal.pending); assertFalse(c.snapshot(302000).canStart);
    }
    @Test public void timeoutCancellationFailureKeepsStopAndJournalUntilManualRetry() {
        FakeVehicle car=new FakeVehicle(); Journal journal=new Journal();
        PreconditioningController c=ready(car,journal); c.start(1000); car.stopFails=true;
        c.refresh(301000); c.refresh(401000);
        assertEquals(1,car.stops); assertTrue(journal.pending); assertTrue(c.snapshot(401000).canStop);
        assertFalse(c.snapshot(401000).canStart);
        car.stopFails=false; c.stop(402000); c.refresh(403000);
        assertEquals(2,car.stops); assertFalse(journal.pending); assertTrue(c.snapshot(403000).canStart);
    }
    @Test public void lateActivationAfterCancellationCannotRestoreStartUntilOff() {
        FakeVehicle car=new FakeVehicle(); Journal journal=new Journal();
        PreconditioningController c=ready(car,journal); c.start(1000); c.refresh(301000);
        car.reading=new VehiclePort.Reading(2,1,1,-1); c.refresh(302000);
        assertTrue(journal.pending); assertFalse(c.snapshot(302000).canStart);
        car.reading=new VehiclePort.Reading(2,0,1,-1); c.refresh(303000);
        assertFalse(journal.pending); assertTrue(c.snapshot(303000).canStart);
    }
    @Test public void newAttemptAfterTimeoutGetsItsOwnDeadline() {
        FakeVehicle car=new FakeVehicle(); PreconditioningController c=ready(car,new Journal());
        c.start(1000); c.refresh(301000); c.refresh(302000); assertTrue(c.start(400000));
        c.refresh(699999); assertEquals(1,car.stops);
        c.refresh(700000); assertEquals(2,car.stops);
        assertEquals(2,car.starts);
    }
    @Test public void manualStopBeforeDeadlineDoesNotBecomeATimeout() {
        FakeVehicle car=new FakeVehicle(); PreconditioningController c=ready(car,new Journal());
        c.start(1000); c.stop(300000); c.refresh(301000);
        assertEquals(PreconditioningSnapshot.State.ENDED,c.snapshot(301000).state);
        assertEquals(1,car.stops);
    }
    @Test public void activeRequiresTheCarsIndicatorAndItsEndReleasesOwnership() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); c.start(1000);
        car.reading = new VehiclePort.Reading(2, 1, 1, -1); c.refresh(61000);
        assertEquals(PreconditioningSnapshot.State.ACTIVE, c.snapshot(61000).state);
        car.reading = new VehiclePort.Reading(2, 0, 1, -1); c.refresh(62000);
        assertEquals(1, car.stops); c.refresh(63000);
        assertEquals(PreconditioningSnapshot.State.ENDED, c.snapshot(63000).state);
        assertFalse(journal.pending);
        assertEquals(c.snapshot(63000).elapsedMs, c.snapshot(1000000).elapsedMs);
    }
    @Test public void stopDuringBmsDelayCancelsOnceAndWaitsForFeedback() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); c.start(1000);
        c.stop(1100); c.stop(1101);
        assertEquals(1, car.stops); assertTrue(journal.pending);
        assertEquals(PreconditioningSnapshot.State.STOPPING, c.snapshot(1100).state);
        c.refresh(2000); assertFalse(c.snapshot(2000).requested); assertFalse(journal.pending);
    }
    @Test public void duplicateStartNeverSendsAnotherStart() {
        FakeVehicle car = new FakeVehicle(); PreconditioningController c = ready(car, new Journal());
        assertTrue(c.start(1000)); assertFalse(c.start(1001)); assertEquals(1, car.starts);
    }
    @Test public void disconnectedStatusIsUnknownAndKeepsStopAvailable() {
        FakeVehicle car = new FakeVehicle(); PreconditioningController c = ready(car, new Journal());
        c.start(1000); car.readFails = true; c.refresh(121000);
        assertEquals(PreconditioningSnapshot.State.UNKNOWN, c.snapshot(121000).state);
        assertEquals(-1, c.snapshot(121000).indicator); assertTrue(c.snapshot(121000).canStop);
        assertEquals(1, car.starts);
    }
    @Test public void reservedIndicatorIsNeverActive() {
        FakeVehicle car = new FakeVehicle(); PreconditioningController c = ready(car, new Journal());
        c.start(1000); car.reading = new VehiclePort.Reading(2, 2, 1, -1); c.refresh(2000);
        assertEquals(PreconditioningSnapshot.State.UNKNOWN, c.snapshot(2000).state);
        assertEquals("Car indicator: Unknown", c.snapshot(2000).indicatorLabel());
    }
    @Test public void unresolvedProfileCannotTransmitEvenWithEnabledSetting() {
        FakeVehicle car = new FakeVehicle(); car.unavailable = "Unsupported firmware";
        PreconditioningController c = ready(car, new Journal());
        assertFalse(c.start(1000)); assertEquals(0, car.starts);
        assertEquals(PreconditioningSnapshot.State.UNAVAILABLE, c.snapshot(1000).state);
    }
    @Test public void recoveryCancelsUncertainSessionAndNeverRestarts() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal(); journal.pending = true;
        PreconditioningController c = new PreconditioningController(car, journal);
        c.recover(true, 1000); c.refresh(2000);
        assertEquals(0, car.starts); assertEquals(1, car.stops); assertFalse(journal.pending);
    }
    @Test public void cancellationFailureDoesNotClaimTheSessionEnded() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); c.start(1000); car.stopFails = true;
        c.stop(2000); c.refresh(3000);
        assertEquals(PreconditioningSnapshot.State.ERROR, c.snapshot(3000).state);
        assertTrue(c.snapshot(3000).canStop); assertTrue(journal.pending);
        car.stopFails = false; c.stop(4000); c.refresh(5000);
        assertFalse(journal.pending); assertEquals(2, car.stops);
    }
    @Test public void partialStartFailureStillAllowsCancellation() {
        FakeVehicle car = new FakeVehicle(); Journal journal = new Journal();
        PreconditioningController c = ready(car, journal); car.startFails = true;
        assertFalse(c.start(1000)); assertTrue(c.snapshot(1000).canStop); assertTrue(journal.pending);
        c.stop(2000); c.refresh(3000); assertFalse(journal.pending);
    }
    @Test public void failedJournalPreventsAnyTransmission() {
        FakeVehicle car = new FakeVehicle();
        PreconditioningController c = new PreconditioningController(car, value -> { throw new IllegalStateException("Full"); });
        c.refresh(1000); assertFalse(c.start(1000)); assertEquals(0, car.starts);
    }
    @Test public void maintenanceRefreshIsNotAStartRetry() {
        FakeVehicle car = new FakeVehicle(); car.interval = 5000;
        PreconditioningController c = ready(car, new Journal()); c.start(1000);
        c.refresh(2000); assertEquals(0, car.maintenance);
        c.refresh(6000); c.refresh(121000);
        assertEquals(2, car.maintenance); assertEquals(1, car.starts);
        c.stop(121001); c.refresh(126000); assertEquals(2, car.maintenance);
    }
    @Test public void disabledSettingOrNotReadyPreventsStartWithoutChangingPreferences() {
        FakeVehicle car = new FakeVehicle(); car.reading = new VehiclePort.Reading(1, 0, 1, -1);
        PreconditioningController c = ready(car, new Journal()); assertFalse(c.start(1000));
        car.reading = new VehiclePort.Reading(2, 0, 0, -1); c.refresh(2000); assertFalse(c.start(2000));
        assertEquals(0, car.starts);
    }
}
