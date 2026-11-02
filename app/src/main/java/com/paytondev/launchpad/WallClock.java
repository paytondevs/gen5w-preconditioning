package com.paytondev.launchpad;

/**
 * The app's only wall-clock read. Everything else times itself with elapsedRealtime;
 * a schedule is a time of day, so it needs this. tools/test_service_rebind.py swaps
 * this class for a fake clock.
 */
final class WallClock {
    private WallClock() { }
    static long now() { return System.currentTimeMillis(); }
}
