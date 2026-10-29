package com.paytondev.launchpad;

/**
 * Hyundai split view: the map panel is drawn over the right of the screen without
 * resizing this window, so Launchpad pads its own layout. Ioniq 5 NEEV23 US,
 * 2026-10-29: Settings.System split_screen_status is 1 while the panel is open and 0
 * when it closes; the head unit's own apps shrink to fit. The panel's width isn't
 * reported; photos put it near 29% of the screen, so a third is reserved.
 */
final class SplitScreen {
    static final String SETTING = "split_screen_status";

    static boolean on(int status) { return status == 1; }
    /** Right padding that keeps content clear of the panel, in pixels. */
    static int insetPx(int screenWidthPx) { return Math.round(screenWidthPx / 3f); }

    private SplitScreen() { }
}
