package com.paytondev.launchpad;

/** Native cluster minimum pack temperature; unknown values must not cause a warm skip. */
final class BatteryTemperature {
    // Default and EV6 cutoff; profiles may set their own (PreconditioningProfile.warmThresholdC).
    static final int EXPECTED_HEATING_THRESHOLD_C = 20;
    static Integer decode(int[] values) {
        if (values == null || values.length != 8) return null;
        // Plausibility envelope, not a calibration claim. Reject reserved byte
        // values and malformed SOC (index 0, XEV_SOCDIS). Exact profiles only.
        if (values[0] < 0 || values[0] > 200 || values[1] < -40 || values[1] > 60) return null;
        return values[1];
    }
    /**
     * DATC_OUT_TEMP_SNSR, index 4 of the same array: the car's outside-air sensor.
     * raw / 2 − 40 is the usual Hyundai DATC scaling (fixtures: 108 → 14 °C, 113 → 16.5 °C).
     * UNVERIFIED against the car's own display; the scheduler shows it so owners can compare.
     * Only read from an array whose battery fields already decode.
     */
    static Integer decodeOutside(int[] values) {
        if (decode(values) == null) return null;
        int raw = values[4];
        if (raw <= 0 || raw >= 255) return null;
        int celsius = Math.round(raw / 2f - 40f);
        return celsius < -40 || celsius > 60 ? null : celsius;
    }
    /** One cluster reply: pack minimum, optional pack maximum, outside air. Any may be null. */
    static final class Sample {
        static final Sample EMPTY = new Sample(null, null, null);
        final Integer minimumC, maximumC, outsideC;
        Sample(Integer minimumC, Integer maximumC, Integer outsideC) {
            this.minimumC = minimumC; this.maximumC = maximumC; this.outsideC = outsideC;
        }
    }
    static boolean isWarm(Integer celsius, int thresholdC) {
        return celsius != null && celsius >= thresholdC && celsius <= 60;
    }
}
