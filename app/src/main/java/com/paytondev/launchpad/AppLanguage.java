package com.paytondev.launchpad;

/** Explicit UI language preference; never changes the device language. */
enum AppLanguage {
    ENGLISH, KOREAN;

    static AppLanguage fromPreference(String value) {
        return "KOREAN".equals(value) ? KOREAN : ENGLISH;
    }

    AppLanguage next() { return this == ENGLISH ? KOREAN : ENGLISH; }
    String choose(String english, String korean) { return this == KOREAN ? korean : english; }
}
