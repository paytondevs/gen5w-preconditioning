package com.paytondev.launchpad;

/** Display-only preference for the battery screen; all sensor values remain Celsius. */
enum BatteryDisplayUnit {
    FAHRENHEIT("°F"), CELSIUS("°C");

    final String symbol;
    BatteryDisplayUnit(String symbol) { this.symbol = symbol; }

    static BatteryDisplayUnit fromPreference(String value) {
        for (BatteryDisplayUnit unit : values()) if (unit.name().equals(value)) return unit;
        return FAHRENHEIT;
    }
    BatteryDisplayUnit other() { return this == FAHRENHEIT ? CELSIUS : FAHRENHEIT; }
    int value(float celsius) {
        return this == CELSIUS ? Math.round(celsius) : BatteryTemperatureReading.fahrenheit(celsius);
    }
    String format(float celsius) { return value(celsius) + symbol; }
}
