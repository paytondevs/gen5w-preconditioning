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
        assertEquals("Starting in 3:28:50", ScheduleFormat.sendLine(s, TEN));
        assertEquals("Waiting for the car", ScheduleFormat.sendLine(s, s.plannedSendAt));
        PreconditioningSchedule warm = PreconditioningSchedule.arriveBy(TEN + 60 * MIN, TEN, 25, 10, 21);
        assertEquals("Battery already warm", ScheduleFormat.sendLine(warm, TEN));
    }
    @Test public void eventLineLeadsWithTimeAndKind() {
        ScheduleLog.Event e = ScheduleLog.EMPTY.add(TEN, ScheduleLog.Kind.ARMED, "Start by 10:30 AM").latest();
        assertEquals("10:00 AM  Scheduled · Start by 10:30 AM", ScheduleFormat.event(e, TEN + MIN, false, UTC));
    }
    @Test public void temperatureShowsFahrenheitFirst() {
        assertEquals("48°F (9°C)", ScheduleFormat.temperature(9));
        assertEquals("—", ScheduleFormat.temperature(null));
    }
    @Test public void currentPlanReasonUsesSelectedUnitWithoutChangingStoredText() {
        String original = "About 15 min to warm the battery from 48°F to 70°F.";
        String display = ScheduleFormat.displayPlanReason(original, BatteryDisplayUnit.CELSIUS);
        assertEquals("About 15 min to warm the battery from 9°C to 21°C.", display);
        assertEquals("About 15 min to warm the battery from 48°F to 70°F.", original);
        assertEquals(original, ScheduleFormat.displayPlanReason(original, BatteryDisplayUnit.FAHRENHEIT));
        assertEquals(display, ScheduleFormat.displayPlanReason(display, BatteryDisplayUnit.CELSIUS));
        String capped = "About 30 min to warm the battery from -4°F to 70°F. It may not be fully warm in time.";
        assertEquals("About 30 min to warm the battery from -20°C to 21°C. It may not be fully warm in time.",
            ScheduleFormat.displayPlanReason(capped, BatteryDisplayUnit.CELSIUS));
        String malformed = "About 15 min to warm the battery from 4.8°F to 70°F.";
        assertEquals(malformed, ScheduleFormat.displayPlanReason(malformed, BatteryDisplayUnit.CELSIUS));
        assertEquals("9°C (48°F)", ScheduleFormat.displayPlanReason("9°C (48°F)", BatteryDisplayUnit.CELSIUS));
        assertNull(ScheduleFormat.displayPlanReason(null, BatteryDisplayUnit.CELSIUS));
    }
    @Test public void hourWheelKeepsMinutesAndWrapsTheDay() {
        assertEquals(8 * 60 + 45, ScheduleFormat.stepHour(7 * 60 + 45, 1));
        assertEquals(23 * 60 + 45, ScheduleFormat.stepHour(45, -1));
        assertEquals(10, ScheduleFormat.stepHour(23 * 60 + 10, 1));
    }
    @Test public void minuteWheelStepsFiveMinuteMarksWithinTheHour() {
        assertEquals(7 * 60 + 50, ScheduleFormat.stepMinute(7 * 60 + 45, 1));
        assertEquals(7 * 60, ScheduleFormat.stepMinute(7 * 60 + 55, 1));
        assertEquals(7 * 60 + 55, ScheduleFormat.stepMinute(7 * 60, -1));
        // An armed time off the five-minute grid snaps to the next mark down, then steps.
        assertEquals(7 * 60 + 50, ScheduleFormat.stepMinute(7 * 60 + 47, 1));
        assertEquals(7 * 60 + 45, ScheduleFormat.stepMinute(7 * 60 + 47, 0));
    }
    @Test public void morningAndAfternoonKeepTheClockTime() {
        assertEquals(19 * 60 + 45, ScheduleFormat.withAfternoon(7 * 60 + 45, true));
        assertEquals(7 * 60 + 45, ScheduleFormat.withAfternoon(19 * 60 + 45, false));
        assertEquals(12 * 60 + 5, ScheduleFormat.withAfternoon(5, true));
        assertEquals(7 * 60 + 45, ScheduleFormat.withAfternoon(7 * 60 + 45, false));
    }
    @Test public void wheelLabelsFollowTheClockFormat() {
        assertEquals("12", ScheduleFormat.hourLabel(5, false));
        assertEquals("7", ScheduleFormat.hourLabel(19 * 60, false));
        assertEquals("12", ScheduleFormat.hourLabel(12 * 60, false));
        assertEquals("00", ScheduleFormat.hourLabel(5, true));
        assertEquals("19", ScheduleFormat.hourLabel(19 * 60, true));
        assertEquals("05", ScheduleFormat.minuteLabel(7 * 60 + 5));
    }
}
