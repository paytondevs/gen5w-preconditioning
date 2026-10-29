package com.paytondev.launchpad;

/** Advanced → Appearance. Follow car mirrors the head unit's day/night state. */
enum AppearanceMode {
    FOLLOW_CAR("Follow car"), LIGHT("Light"), DARK("Dark");
    final String title;
    AppearanceMode(String title) { this.title = title; }

    static AppearanceMode fromPreference(String value) {
        for (AppearanceMode mode : values()) if (mode.name().equals(value)) return mode;
        return FOLLOW_CAR;
    }
    boolean dark(boolean carIsDark) {
        return this == DARK || (this == FOLLOW_CAR && carIsDark);
    }
}
