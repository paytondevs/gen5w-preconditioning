package com.paytondev.launchpad;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Wall-clock text for the scheduler. The zone and 12/24-hour choice are passed in. */
final class ScheduleFormat {
    private ScheduleFormat() { }

    static String time(long epochMs, boolean twentyFourHour, TimeZone zone) {
        return format(twentyFourHour ? "H:mm" : "h:mm a", epochMs, zone);
    }
    static String timeWithSeconds(long epochMs, boolean twentyFourHour, TimeZone zone) {
        return format(twentyFourHour ? "H:mm:ss" : "h:mm:ss a", epochMs, zone);
    }
    private static String format(String pattern, long epochMs, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        format.setTimeZone(zone);
        return format.format(new Date(epochMs));
    }
    /** "4:05", "1:02:03"; rounds up so a pending send never reads 0:00. */
    static String countdown(long remainingMs) {
        long seconds = Math.max(0, (remainingMs + 999) / 1000);
        long hours = seconds / 3600, minutes = seconds / 60 % 60, rest = seconds % 60;
        return hours > 0 ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, rest)
            : String.format(Locale.US, "%d:%02d", minutes, rest);
    }
    /** "" today, " tomorrow", otherwise " Wed". */
    static String dayHint(long epochMs, long nowEpochMs, TimeZone zone) {
        long days = dayNumber(epochMs, zone) - dayNumber(nowEpochMs, zone);
        if (days == 0) return "";
        if (days == 1) return " tomorrow";
        return " " + format("EEE", epochMs, zone);
    }
    private static long dayNumber(long epochMs, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.US); c.setTimeInMillis(epochMs);
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR);
    }
    static int minuteOfDay(long epochMs, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone, Locale.US); c.setTimeInMillis(epochMs);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }
    /** Next time the clock reads this minute of the day, strictly after now. */
    static long nextOccurrence(int minuteOfDay, long nowEpochMs, TimeZone zone) {
        int minute = wrapDay(minuteOfDay);
        Calendar c = Calendar.getInstance(zone, Locale.US); c.setTimeInMillis(nowEpochMs);
        for (int day = 0; day < 3; day++) {
            Calendar candidate = (Calendar) c.clone();
            candidate.add(Calendar.DATE, day);
            candidate.set(Calendar.HOUR_OF_DAY, minute / 60); candidate.set(Calendar.MINUTE, minute % 60);
            candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
            if (candidate.getTimeInMillis() > nowEpochMs) return candidate.getTimeInMillis();
        }
        return nowEpochMs + 24 * 60 * 60_000L;
    }
    /** The next whole five minutes at or after this minute of the day, wrapping at midnight. */
    static int roundUpToFive(int minuteOfDay) {
        return wrapDay((minuteOfDay + 4) / 5 * 5);
    }
    /** Minute of the day in 0..1439 for any whole-minute offset (Math.floorMod is API 24). */
    static int wrapDay(int minutes) { int day = 24 * 60; return ((minutes % day) + day) % day; }
    /** The monitor's countdown line for an armed schedule. */
    static String sendLine(PreconditioningSchedule schedule, long nowEpochMs) {
        if (schedule.plannedSendAt == PreconditioningSchedule.NONE) return "Battery already warm";
        if (nowEpochMs >= schedule.plannedSendAt) return "Waiting for the car";
        return "Starting in " + countdown(schedule.plannedSendAt - nowEpochMs);
    }
    /** "1:02 PM  Scheduled · Start by 1:30 PM" */
    static String event(ScheduleLog.Event event, long nowEpochMs, boolean twentyFourHour, TimeZone zone) {
        return time(event.at, twentyFourHour, zone) + dayHint(event.at, nowEpochMs, zone) + "  "
            + event.kind.label() + " · " + event.text;
    }
    /** "48°F (9°C)" or "—". */
    static String temperature(Integer celsius) {
        return celsius == null ? "—" : BatteryTemperatureReading.fahrenheit(celsius) + "°F (" + celsius + "°C)";
    }
}
