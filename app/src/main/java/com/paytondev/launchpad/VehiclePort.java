package com.paytondev.launchpad;

/** Only the firmware profile may translate these operations into OEM commands. */
interface VehiclePort {
    final class Reading {
        final int enabled, indicator, ready, failureRaw;
        Reading(int enabled, int indicator, int ready, int failureRaw) {
            this.enabled = enabled; this.indicator = indicator;
            this.ready = ready; this.failureRaw = failureRaw;
        }
    }
    String unavailableReason();
    Reading read() throws Exception;
    /** Optional one-shot precheck. Unknown never prevents a manual request. */
    default Integer readMinimumBatteryTemperatureC() { return null; }
    /** Display-only; never used for the warm advisory. */
    default Integer readMaximumBatteryTemperatureC() { return null; }
    void beginManual() throws Exception;
    void maintainManual() throws Exception;
    void stopAndRelease() throws Exception;
    long maintenanceIntervalMs();
    /** Cancel a request the car has not reported Active within this time. */
    default long activationTimeoutMs() { return PreconditioningController.ACTIVATION_TIMEOUT_MS; }
}
