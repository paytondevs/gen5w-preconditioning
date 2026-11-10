package com.paytondev.launchpad;

/** Display-only preference for the battery screen; all sensor values remain Celsius. */
enum BatteryDisplayUnit {
    FAHRENHEIT("°F"), CELSIUS("°C");

    private static final java.util.regex.Pattern WARM_DETAIL = java.util.regex.Pattern.compile(
        "(?s)^Battery minimum (-?[0-9]{1,3})°C is at or above the expected heating threshold \\((-?[0-9]{1,3})°C / -?[0-9]{1,3}°F\\)(.*)$");
    final String symbol;
    BatteryDisplayUnit(String symbol) { this.symbol = symbol; }

    int value(float celsius) {
        return this == CELSIUS ? Math.round(celsius) : BatteryTemperatureReading.fahrenheit(celsius);
    }
    String format(float celsius) { return value(celsius) + symbol; }
    String formatNullable(Integer celsius) { return celsius == null ? "—" : format(celsius); }

    /** Reformat only the current warm-status text; leave stored events untouched. */
    String displayCurrentDetail(String detail) {
        if (detail == null) return null;
        java.util.regex.Matcher m = WARM_DETAIL.matcher(detail);
        if (!m.matches()) return detail;
        return "Battery minimum " + format(Integer.parseInt(m.group(1)))
            + " is at or above the expected heating threshold (" + format(Integer.parseInt(m.group(2))) + ")" + m.group(3);
    }
}
