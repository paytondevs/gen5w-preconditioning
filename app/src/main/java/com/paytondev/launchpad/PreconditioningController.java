package com.paytondev.launchpad;

/** Single-writer controller. Time updates the display; it never implies activation. */
final class PreconditioningController {
    static final long ACTIVATION_TIMEOUT_MS = 5 * 60 * 1000L;
    static final long WARM_NOTICE_MS = 10 * 1000L;
    interface Journal { void requested(boolean value); }
    private final VehiclePort vehicle;
    private final Journal journal;
    private VehiclePort.Reading reading;
    private boolean requested, activeSeen, stopping, released, commandFailed, activationTimedOut;
    private long startedAt, endedAt, nextMaintenance;
    private long warmUntil;
    private int warmTemperatureC;
    private PreconditioningSnapshot.State state = PreconditioningSnapshot.State.CHECKING;
    private String detail = "Checking vehicle services";
    private String commandError = "";

    PreconditioningController(VehiclePort vehicle, Journal journal) {
        this.vehicle = vehicle; this.journal = journal;
    }
    void recover(boolean pending, long now) {
        if (pending) {
            requested = true; startedAt = now;
            // A recovered process must cancel uncertain ownership, never send Start.
            stop(now);
        }
    }
    void refresh(long now) {
        if (warmUntil != 0 && now >= warmUntil) {
            warmUntil = 0;
            state = PreconditioningSnapshot.State.CHECKING;
        }
        try { reading = vehicle.read(); }
        catch (Exception e) {
            reading = null;
            if (cancelExpiredRequest(now)) return;
            state = PreconditioningSnapshot.State.UNKNOWN;
            detail = "Vehicle status unavailable. " + message(e);
            return;
        }
        // A fresh Active indication wins at the deadline; active sessions have no
        // five-minute cutoff. Unknown/missing feedback must not prolong a pending
        // request forever, but cancellation never implies confirmed Off.
        if (requested && !stopping && reading.indicator == 1) activeSeen = true;
        if (cancelExpiredRequest(now)) return;
        if (reading.indicator != 0 && reading.indicator != 1) {
            state = PreconditioningSnapshot.State.UNKNOWN;
            detail = "The car has not supplied a recognized conditioning indicator.";
            return;
        }
        if (stopping) {
            if (released && reading.indicator == 0) {
                try { journal.requested(false); }
                catch (RuntimeException e) { fail("Cancel sent, but session completion could not be recorded. ", e); return; }
                requested = false; stopping = false;
                endedAt = now;
                state = activationTimedOut ? PreconditioningSnapshot.State.TIMED_OUT : PreconditioningSnapshot.State.ENDED;
                detail = activationTimedOut
                    ? "No activation confirmed within " + timeoutText() + ". Cancellation confirmed; the car reports Off."
                    : "Cancel sent; the car reports conditioning Off.";
            } else if (released) {
                state = PreconditioningSnapshot.State.STOPPING;
                detail = "Cancel sent; waiting for the car’s Off indicator.";
            }
            return;
        }
        if (requested) {
            if (reading.ready == 0 || reading.enabled == 1 || (activeSeen && reading.indicator == 0)) {
                stop(now);
                return;
            }
            if (commandFailed) {
                state = PreconditioningSnapshot.State.ERROR; detail = commandError; return;
            }
            if (reading.ready != 1 || reading.enabled != 2) {
                state = PreconditioningSnapshot.State.UNKNOWN;
                detail = "Vehicle operating status is unknown. Stop remains available.";
                return;
            }
            if (reading.indicator == 1) {
                activeSeen = true; state = PreconditioningSnapshot.State.ACTIVE;
                detail = "The car reports battery conditioning active.";
            } else {
                state = PreconditioningSnapshot.State.WAITING;
                detail = waitingDetail();
            }
            // Refresh the OEM request metadata. Never resend the Start pulse.
            long interval = vehicle.maintenanceIntervalMs();
            if (interval > 0 && now >= nextMaintenance) {
                try { vehicle.maintainManual(); nextMaintenance = now + interval; }
                catch (Exception e) { fail("Manual request maintenance failed. ", e); }
            }
            return;
        }
        if (warmUntil > now && reading.indicator == 0 && reading.ready == 1
                && reading.enabled == 2 && vehicle.unavailableReason().isEmpty()) {
            state = PreconditioningSnapshot.State.WARM;
            return;
        }
        warmUntil = 0;
        if (state == PreconditioningSnapshot.State.ENDED || state == PreconditioningSnapshot.State.TIMED_OUT) return;
        String unavailable = vehicle.unavailableReason();
        if (!unavailable.isEmpty()) {
            state = PreconditioningSnapshot.State.UNAVAILABLE; detail = unavailable;
        } else if (reading.enabled != 2) {
            state = PreconditioningSnapshot.State.UNAVAILABLE;
            detail = "Enable battery conditioning in the vehicle’s EV settings first.";
        } else if (reading.ready != 1) {
            state = PreconditioningSnapshot.State.UNAVAILABLE;
            detail = "The car has not reported READY.";
        } else {
            state = PreconditioningSnapshot.State.IDLE;
            detail = reading.indicator == 1 ? "Conditioning is controlled by the car."
                : "Start a manual battery-conditioning request.";
        }
    }
    boolean start(long now) {
        return start(now, null);
    }
    boolean start(long now, Integer minimumTemperatureC) {
        return start(now, minimumTemperatureC, false);
    }
    boolean startAnyway(long now) {
        if (state != PreconditioningSnapshot.State.WARM || warmUntil <= now) return false;
        return start(now, null, true);
    }
    private boolean start(long now, Integer minimumTemperatureC, boolean requestAnyway) {
        if (!snapshot(now).canStart) return false;
        // A queued duplicate Start must never become consent to override a warning.
        if (warmUntil > now && !requestAnyway) return false;
        warmUntil = 0;
        if (!requestAnyway && BatteryTemperature.isWarm(minimumTemperatureC)) {
            warmUntil = now + WARM_NOTICE_MS;
            warmTemperatureC = minimumTemperatureC;
            endedAt = 0;
            state = PreconditioningSnapshot.State.WARM;
            // This is a local advisory: do not journal, Start, maintain or cancel.
            return false;
        }
        requested = true; stopping = false; released = false; activeSeen = false; commandFailed = false; activationTimedOut = false;
        startedAt = now; endedAt = 0;
        try {
            journal.requested(true); // Record uncertain ownership before any transmission.
        } catch (RuntimeException e) {
            requested = false; fail("Start was not sent because session ownership could not be recorded. ", e);
            return false;
        }
        try {
            vehicle.beginManual();
            nextMaintenance = now + vehicle.maintenanceIntervalMs();
            state = PreconditioningSnapshot.State.WAITING;
            detail = waitingDetail();
            return true;
        } catch (Exception e) { fail("Start could not be confirmed. Stop remains available. ", e); return false; }
    }
    private String waitingDetail() {
        return "Request sent; conditioning is not active. Vehicle conditions may prevent activation. Cancels after "
            + timeoutText() + " without confirmed activation.";
    }
    private String timeoutText() {
        long minutes = vehicle.activationTimeoutMs() / 60000;
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
    private boolean cancelExpiredRequest(long now) {
        if (requested && !stopping && !activeSeen && now - startedAt >= vehicle.activationTimeoutMs()) {
            activationTimedOut = true;
            stop(now);
            return true;
        }
        return false;
    }
    void stop(long now) {
        if (!requested || (stopping && released)) return;
        stopping = true;
        state = PreconditioningSnapshot.State.STOPPING;
        detail = activationTimedOut
            ? "No activation confirmed within " + timeoutText() + ". Cancelling; waiting for the car’s Off indicator."
            : "Releasing the manual request; waiting for the car’s Off indicator.";
        try { vehicle.stopAndRelease(); released = true; }
        catch (Exception e) { fail("Cancellation is unconfirmed. Stop can be tried again. ", e); }
    }
    PreconditioningSnapshot snapshot(long now) {
        int indicator = reading == null ? -1 : reading.indicator;
        boolean known = reading != null && (indicator == 0 || indicator == 1);
        boolean canStart = !requested && known && indicator == 0 && reading.enabled == 2
            && reading.ready == 1 && vehicle.unavailableReason().isEmpty();
        long resetSeconds = state == PreconditioningSnapshot.State.WARM && warmUntil > now
            ? (warmUntil - now + 999) / 1000 : 0;
        String displayDetail = resetSeconds > 0
            ? "Battery minimum " + warmTemperatureC + "°C is at or above the expected heating threshold (20°C / 68°F). No request sent. Button resets in "
                + resetSeconds + " seconds. Start anyway sends a manual request; the car decides whether to heat."
            : detail;
        // A successful read passed the firmware gate; read errors only name firmware when it failed.
        int supported = !vehicle.unavailableReason().isEmpty() ? PreconditioningSnapshot.FAIL
            : reading != null ? PreconditioningSnapshot.PASS
            : state == PreconditioningSnapshot.State.UNKNOWN && detail.contains("firmware") ? PreconditioningSnapshot.FAIL
            : PreconditioningSnapshot.UNKNOWN;
        return new PreconditioningSnapshot(state, requested, canStart,
            requested && !(stopping && released), indicator,
            reading == null ? -1 : reading.failureRaw,
            requested ? Math.max(0, now - startedAt) : endedAt > 0 ? Math.max(0, endedAt - startedAt) : 0,
            displayDetail, resetSeconds, reading == null ? -1 : reading.ready,
            reading == null ? -1 : reading.enabled, supported);
    }
    private void fail(String prefix, Exception e) {
        commandFailed = true;
        state = PreconditioningSnapshot.State.ERROR; detail = prefix + message(e); commandError = detail;
    }
    private static String message(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
