package com.paytondev.launchpad;

import java.lang.reflect.Method;
import java.util.Locale;

/** Battery screen unit preference. Automatic is a display choice, not a vehicle setting. */
enum BatteryDisplayPreference {
    AUTO, CELSIUS, FAHRENHEIT;

    static BatteryDisplayPreference fromPreference(String value) {
        for (BatteryDisplayPreference mode : values()) if (mode.name().equals(value)) return mode;
        return AUTO;
    }

    BatteryDisplayPreference next() { return values()[(ordinal() + 1) % values().length]; }

    BatteryDisplayUnit resolve(String product) {
        if (this == CELSIUS) return BatteryDisplayUnit.CELSIUS;
        if (this == FAHRENHEIT) return BatteryDisplayUnit.FAHRENHEIT;
        // This is a firmware market, not the driver's physical location or locale.
        // Only markets in the supported vehicle reports have established defaults.
        String[] parts = product == null ? new String[0] : product.toUpperCase(Locale.US).split("\\.", -1);
        return parts.length >= 3 && parts[0].matches("[A-Z0-9_]+") && !parts[2].isEmpty() && "KOR".equals(parts[1])
                ? BatteryDisplayUnit.CELSIUS : BatteryDisplayUnit.FAHRENHEIT;
    }

    String title(BatteryDisplayUnit current) {
        return this == AUTO ? "Auto (" + current.symbol + ")" : current.symbol;
    }

    static String vehicleProduct() {
        try {
            Method get = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            Object value = get.invoke(null, "ro.build.product");
            return value instanceof String ? (String) value : "";
        } catch (Exception ignored) {
            // Property access may be unavailable; keep the historical Fahrenheit default.
            return "";
        }
    }
}
