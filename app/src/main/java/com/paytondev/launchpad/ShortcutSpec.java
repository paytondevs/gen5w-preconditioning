package com.paytondev.launchpad;

final class ShortcutSpec {
    final String title;
    final String component;
    final String action;

    private ShortcutSpec(String title, String component, String action) {
        this.title = title; this.component = component; this.action = action;
    }

    static final ShortcutSpec NEMESIS = new ShortcutSpec("NEMESIS",
        "com.nemesis.manager/.MainActivity", "android.intent.action.MAIN");
    static final ShortcutSpec WIFI = new ShortcutSpec("Wi-Fi settings", null, "android.settings.WIFI_SETTINGS");

    static boolean mayLaunch(boolean exists, boolean exported, boolean enabled,
                             boolean appEnabled, boolean permitted) {
        return exists && exported && enabled && appEnabled && permitted;
    }
}
