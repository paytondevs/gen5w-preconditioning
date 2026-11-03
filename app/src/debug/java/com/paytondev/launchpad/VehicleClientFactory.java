package com.paytondev.launchpad;

import android.os.Build;
import android.os.SystemClock;

/** Explicit emulator-only simulation, excluded from the release APK. */
final class VehicleClientFactory {
    static VehiclePort create() {
        if (!Build.HARDWARE.equals("ranchu") && !Build.HARDWARE.equals("goldfish"))
            return new OemVehicleClient();
        return new VehiclePort() {
            final long created = SystemClock.elapsedRealtime();
            boolean requested;
            @Override public String unavailableReason() { return ""; }
            @Override public Reading read() { return new Reading(2, 0, 1, -1); }
            // Fake pack temperature for the preview: 8°C, rising 1°C a minute to 24°C.
            @Override public Integer readMinimumBatteryTemperatureC() {
                return (int) Math.min(24, 8 + (SystemClock.elapsedRealtime() - created) / 60000);
            }
            @Override public Integer readMaximumBatteryTemperatureC() { return readMinimumBatteryTemperatureC() + 3; }
            // Fixed 5°C outside air so arrive-by planning has an input in the emulator.
            @Override public BatteryTemperature.Sample readTemperatures() {
                Integer minimum = readMinimumBatteryTemperatureC();
                return new BatteryTemperature.Sample(minimum, minimum + 3, 5);
            }
            @Override public void beginManual() { requested = true; }
            @Override public void maintainManual() { }
            @Override public void stopAndRelease() { requested = false; }
            @Override public long maintenanceIntervalMs() { return 0; }
        };
    }
}
