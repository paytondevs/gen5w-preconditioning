package com.paytondev.launchpad;

import java.util.Locale;

/** Display-only sample. Receipt age is not the vehicle sensor's own timestamp. */
final class BatteryTemperatureReading {
    static final long REFRESH_MS = 10_000;
    static final long STALE_MS = 30_000;
    final Integer minimumC;
    final long receivedAt;
    final boolean attempted, failed;
    private BatteryTemperatureReading(Integer minimumC, long receivedAt, boolean attempted, boolean failed) {
        this.minimumC = minimumC; this.receivedAt = receivedAt;
        this.attempted = attempted; this.failed = failed;
    }
    static BatteryTemperatureReading checking() {
        return new BatteryTemperatureReading(null, 0, false, false);
    }
    BatteryTemperatureReading update(Integer minimum, long now) {
        if (minimum == null || minimum < -40 || minimum > 60)
            return new BatteryTemperatureReading(minimumC, receivedAt, true, true);
        return new BatteryTemperatureReading(minimum, now, true, false);
    }
    boolean stale(long now) {
        return minimumC != null && (failed || now < receivedAt || now - receivedAt >= STALE_MS);
    }
    static int fahrenheit(float celsius) { return Math.round(celsius * 9f / 5f + 32f); }
    String age(long now) {
        if (minimumC == null) return attempted ? "Unavailable" : "Checking…";
        long seconds = Math.max(0, now - receivedAt) / 1000;
        return (stale(now) ? "Stale · last received " : "Received ") + seconds + "s ago";
    }
    String label(long now, boolean compact) {
        String prefix = compact ? "Battery temp: " : "Lowest Battery Pack Temperature: ";
        if (minimumC == null) return prefix + (attempted ? "unavailable" : "checking…");
        String value = String.format(Locale.US, "%d°F (%d°C)", Math.round(minimumC * 9f / 5f + 32f), minimumC);
        // Keep stale visible in the narrow Home row; Advanced retains last value.
        if (compact) return prefix + (stale(now) ? "stale"
            : String.format(Locale.US, "%d°F", Math.round(minimumC * 9f / 5f + 32f)));
        long seconds = Math.max(0, now - receivedAt) / 1000;
        return prefix + value + (stale(now) ? " · stale; last received " : " · received ") + seconds + "s ago";
    }
}
