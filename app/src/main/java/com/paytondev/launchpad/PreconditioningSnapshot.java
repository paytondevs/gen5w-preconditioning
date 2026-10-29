package com.paytondev.launchpad;

import java.util.Locale;

/** Immutable UI state. A pending request and the vehicle indicator are independent. */
final class PreconditioningSnapshot {
    enum State { CHECKING, UNAVAILABLE, IDLE, WARM, WAITING, ACTIVE, STOPPING, ENDED, TIMED_OUT, UNKNOWN, ERROR }
    final State state;
    final boolean requested, canStart, canStop;
    static final int PASS = 1, FAIL = 0, UNKNOWN = -1;
    final int indicator, failureRaw;
    /** Raw vehicle values, or -1 before a successful read. {@code supported} is a check result. */
    final int ready, enabled, supported;
    final long elapsedMs;
    final long resetSeconds;
    final String detail;
    PreconditioningSnapshot(State state, boolean requested, boolean canStart, boolean canStop,
                            int indicator, int failureRaw, long elapsedMs, String detail) {
        this(state, requested, canStart, canStop, indicator, failureRaw, elapsedMs, detail, 0);
    }
    PreconditioningSnapshot(State state, boolean requested, boolean canStart, boolean canStop,
                            int indicator, int failureRaw, long elapsedMs, String detail, long resetSeconds) {
        this(state, requested, canStart, canStop, indicator, failureRaw, elapsedMs, detail, resetSeconds, -1, -1, UNKNOWN);
    }
    PreconditioningSnapshot(State state, boolean requested, boolean canStart, boolean canStop,
                            int indicator, int failureRaw, long elapsedMs, String detail, long resetSeconds,
                            int ready, int enabled, int supported) {
        this.state = state; this.requested = requested; this.canStart = canStart;
        this.canStop = canStop; this.indicator = indicator; this.failureRaw = failureRaw;
        this.elapsedMs = elapsedMs; this.detail = detail;
        this.resetSeconds = resetSeconds;
        this.ready = ready; this.enabled = enabled; this.supported = supported;
    }
    static PreconditioningSnapshot checking() {
        return new PreconditioningSnapshot(State.CHECKING, false, false, false, -1, -1, 0,
            "Checking vehicle services");
    }
    String title() {
        switch (state) {
            case CHECKING: return "Checking";
            case UNAVAILABLE: return "Unavailable";
            case IDLE: return indicator == 1 ? "Active · car controlled" : "Ready";
            case WARM: return "Battery warm · resets in " + resetSeconds + "s";
            case WAITING: return "Request sent";
            case ACTIVE: return "Active";
            case STOPPING: return "Stop requested";
            case ENDED: return "Session ended";
            case TIMED_OUT: return "No activation";
            case UNKNOWN: return "Status unknown";
            default: return "Request error";
        }
    }
    String elapsed() {
        long seconds = Math.max(0, elapsedMs) / 1000;
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }
    String indicatorLabel() {
        return indicator == 1 ? "Car indicator: Active" : indicator == 0
            ? "Car indicator: Off" : "Car indicator: Unknown";
    }
    int readyCheck() { return ready == 1 ? PASS : ready == 0 ? FAIL : UNKNOWN; }
    int enabledCheck() { return enabled == 2 ? PASS : enabled >= 0 ? FAIL : UNKNOWN; }
    int indicatorCheck() { return indicator == 0 || indicator == 1 ? PASS : UNKNOWN; }
    String readyValue() { return ready == 1 ? "Reported READY" : ready == 0 ? "Not READY" : unknown(ready); }
    String enabledValue() {
        return enabled == 2 ? "On in EV settings" : enabled >= 0 ? "Off in EV settings (raw " + enabled + ")" : "Unknown";
    }
    String indicatorValue() { return indicator == 1 ? "Active" : indicator == 0 ? "Off" : unknown(indicator); }
    String supportedValue() {
        return supported == PASS ? "Supported" : supported == FAIL ? "Not supported" : state == State.CHECKING ? "Checking" : "Unknown";
    }
    private static String unknown(int raw) { return raw < 0 ? "Unknown" : "Unknown (raw " + raw + ")"; }
}
