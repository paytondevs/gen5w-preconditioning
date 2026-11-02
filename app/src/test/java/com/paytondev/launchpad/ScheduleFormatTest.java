package com.paytondev.launchpad;

import org.junit.Test;
import java.util.TimeZone;
import static org.junit.Assert.*;

public class ScheduleFormatTest {
    static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    static final long TEN = 1793613600000L;   // 2026-11-02 10:00 UTC (a Monday)
    static final long MIN = 60_000L;

    @Test public void formatsTwelveAndTwentyFourHourTimes() {
        long t = TEN + 3 * 60 * MIN + 30 * MIN;   // 13:30
        assertEquals("1:30 PM", ScheduleFormat.time(t, false, UTC));
        assertEquals("13:30", ScheduleFormat.time(t, true, UTC));
        assertEquals("1:28:50 PM", ScheduleFormat.timeWithSeconds(t - 70_000, false, UTC));
        assertEquals("12:00 AM", ScheduleFormat.time(TEN - 10 * 60 * MIN, false, UTC));
    }
    @Test public void countdownRoundsUp() {
        assertEquals("0:00", ScheduleFormat.countdown(0));
        assertEquals("0:00", ScheduleFormat.countdown(-5000));
        assertEquals("0:01", ScheduleFormat.countdown(1));
        assertEquals("42:17", ScheduleFormat.countdown(42 * MIN + 17_000));
        assertEquals("1:02:03", ScheduleFormat.countdown(62 * MIN + 3_000));
    }
    @Test public void nextOccurrenceIsTodayOrTomorrow() {
        assertEquals(TEN + 30 * MIN, ScheduleFormat.nextOccurrence(10 * 60 + 30, TEN, UTC));
        assertEquals(TEN + 23 * 60 * MIN, ScheduleFormat.nextOccurrence(9 * 60, TEN, UTC));
        assertEquals(TEN + 24 * 60 * MIN, ScheduleFormat.nextOccurrence(10 * 60, TEN, UTC));   // strictly after now
        assertEquals(TEN + 30 * MIN, ScheduleFormat.nextOccurrence(10 * 60 + 30 + 24 * 60, TEN, UTC));
    }
    @Test public void minutesWrapAndRoundToFive() {
        assertEquals(605, ScheduleFormat.roundUpToFive(601));
        assertEquals(600, ScheduleFormat.roundUpToFive(600));
        assertEquals(0, ScheduleFormat.roundUpToFive(1437));
        assertEquals(1435, ScheduleFormat.wrapDay(-5));
        assertEquals(30, ScheduleFormat.wrapDay(24 * 60 + 30));
        assertEquals(10 * 60, ScheduleFormat.minuteOfDay(TEN, UTC));
    }
    @Test public void dayHintNamesTomorrowAndLaterDays() {
        assertEquals("", ScheduleFormat.dayHint(TEN + 60 * MIN, TEN, UTC));
        assertEquals(" tomorrow", ScheduleFormat.dayHint(TEN + 20 * 60 * MIN, TEN, UTC));
        assertEquals(" Wed", ScheduleFormat.dayHint(TEN + 48 * 60 * MIN, TEN, UTC));
    }
    @Test public void sendLineCountsDownThenWaits() {
        PreconditioningSchedule s = PreconditioningSchedule.startBy(TEN + 3 * 60 * MIN + 30 * MIN, TEN);
        assertEquals("Sends at 1:28:50 PM · in 3:28:50", ScheduleFormat.sendLine(s, TEN, false, UTC));
        assertEquals("Send time reached · waiting for the car", ScheduleFormat.sendLine(s, s.plannedSendAt, false, UTC));
        PreconditioningSchedule tomorrow = PreconditioningSchedule.startBy(TEN + 20 * 60 * MIN, TEN);
        assertTrue(ScheduleFormat.sendLine(tomorrow, TEN, true, UTC).startsWith("Sends tomorrow at 5:58:50"));
        PreconditioningSchedule warm = PreconditioningSchedule.arriveBy(TEN + 60 * MIN, TEN, 25, 10, 21);
        assertEquals("Pack already warm · rechecking until 11:00", ScheduleFormat.sendLine(warm, TEN, true, UTC));
    }
    @Test public void eventLineLeadsWithTimeAndKind() {
        ScheduleLog.Event e = ScheduleLog.EMPTY.add(TEN, ScheduleLog.Kind.FIRED, "Start sent").latest();
        assertEquals("10:00 AM  Fired · Start sent", ScheduleFormat.event(e, TEN + MIN, false, UTC));
    }
    @Test public void temperatureShowsFahrenheitFirst() {
        assertEquals("48°F (9°C)", ScheduleFormat.temperature(9));
        assertEquals("—", ScheduleFormat.temperature(null));
    }
}
