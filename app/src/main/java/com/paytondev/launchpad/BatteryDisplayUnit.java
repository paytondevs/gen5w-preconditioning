package com.paytondev.launchpad;

/** Display-only preference for the battery screen; all sensor values remain Celsius. */
enum BatteryDisplayUnit {
    FAHRENHEIT("°F"), CELSIUS("°C");

    final String symbol;
    BatteryDisplayUnit(String symbol) { this.symbol = symbol; }

    BatteryDisplayUnit other() { return this == FAHRENHEIT ? CELSIUS : FAHRENHEIT; }
    int value(float celsius) {
        return this == CELSIUS ? Math.round(celsius) : BatteryTemperatureReading.fahrenheit(celsius);
    }
    String format(float celsius) { return value(celsius) + symbol; }
}
