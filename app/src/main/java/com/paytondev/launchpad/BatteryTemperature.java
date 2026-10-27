package com.paytondev.launchpad;

/** Native minimum pack temperature; unknown values must not cause a warm skip. */
final class BatteryTemperature {
    static final int EXPECTED_HEATING_THRESHOLD_C = 20;
    static Integer decode(int[] values) {
        if (values == null || values.length != 8) return null;
        // Plausibility envelope, not a calibration claim. Reject reserved byte
        // values and malformed SOC (index 0, XEV_SOCDIS). Exact profiles only.
        if (values[0] < 0 || values[0] > 200 || values[1] < -40 || values[1] > 60) return null;
        return values[1];
    }
    static boolean isWarm(Integer celsius) {
        return celsius != null && celsius >= EXPECTED_HEATING_THRESHOLD_C && celsius <= 60;
    }
}
