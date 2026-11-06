package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class PreconditioningScheduleTest {
    static final long NOW = 1793613600000L;   // 2026-11-02 10:00 UTC
    static final long MIN = 60_000L;
    static final long T = NOW + 60 * MIN;     // target an hour out

    @Test public void startBySendsSeventySecondsEarly() {
        PreconditioningSchedule s = PreconditioningSchedule.startBy(T, NOW);
        assertEquals(PreconditioningSchedule.Mode.START_BY, s.mode);
        assertEquals(T - 70_000L, s.plannedSendAt);
        assertEquals(NOW, s.createdAt);
    }
    @Test public void acceptsTwoMinutesToOneDayAhead() {
        assertFalse(PreconditioningSchedule.acceptable(NOW + 2 * MIN - 1, NOW));
        assertTrue(PreconditioningSchedule.acceptable(NOW + 2 * MIN, NOW));
        assertTrue(PreconditioningSchedule.acceptable(NOW + 24 * 60 * MIN, NOW));
        assertFalse(PreconditioningSchedule.acceptable(NOW + 24 * 60 * MIN + 1, NOW));
        assertFalse(PreconditioningSchedule.acceptable(NOW - MIN, NOW));
    }
    @Test public void dueFromSendTimeUntilGraceThenExpired() {
        PreconditioningSchedule s = PreconditioningSchedule.startBy(T, NOW);
        assertEquals(PreconditioningSchedule.Due.WAIT, s.due(s.plannedSendAt - 1));
        assertEquals(PreconditioningSchedule.Due.DUE, s.due(s.plannedSendAt));
        assertEquals(PreconditioningSchedule.Due.DUE, s.due(T + PreconditioningSchedule.GRACE_MS));
        assertEquals(PreconditioningSchedule.Due.EXPIRED, s.due(T + PreconditioningSchedule.GRACE_MS + 1));
        assertEquals(T + PreconditioningSchedule.GRACE_MS, s.deadline());
    }
    @Test public void warmArriveByWaitsUntilArrivalThenExpires() {
        PreconditioningSchedule s = PreconditioningSchedule.arriveBy(T, NOW, 22, 5, 21);
        assertEquals(PreconditioningSchedule.NONE, s.plannedSendAt);
        assertEquals(PreconditioningSchedule.Due.WAIT, s.due(T - 1));
        assertEquals(PreconditioningSchedule.Due.EXPIRED, s.due(T));
    }
    @Test public void arriveByUsesThePlanner() {
        PreconditioningSchedule s = PreconditioningSchedule.arriveBy(T, NOW, 15, 10, 21);
        assertEquals(T - 14 * MIN - 70_000L, s.plannedSendAt);
        assertEquals(Integer.valueOf(15), s.minimumC);
        assertEquals(Integer.valueOf(10), s.outsideC);
    }
    @Test public void startByNeverReplans() {
        PreconditioningSchedule s = PreconditioningSchedule.startBy(T, NOW);
        assertSame(s, s.replanned(NOW + MIN, 0, -20, 21));
    }
    @Test public void arriveByReplansFromFreshReadings() {
        PreconditioningSchedule unknown = PreconditioningSchedule.arriveBy(T, NOW, null, null, 21);
        assertEquals(T - 30 * MIN - 70_000L, unknown.plannedSendAt);
        PreconditioningSchedule known = unknown.replanned(NOW + MIN, 15, 10, 21);
        assertEquals(T - 14 * MIN - 70_000L, known.plannedSendAt);
        assertTrue(known.materiallyDifferent(unknown));
        assertEquals(unknown.createdAt, known.createdAt);
    }
    @Test public void closeToTheSendAPlanOnlyMovesEarlier() {
        PreconditioningSchedule s = PreconditioningSchedule.arriveBy(T, NOW, 15, 10, 21);
        long locked = s.plannedSendAt - 4 * MIN;
        assertSame(s, s.replanned(locked, 19, 10, 21));     // warmer: would send later
        assertSame(s, s.replanned(locked, 22, 10, 21));     // warm: checked at send time instead
        assertTrue(s.replanned(locked, 10, 10, 21).plannedSendAt < s.plannedSendAt);
        assertTrue(s.replanned(s.plannedSendAt - 6 * MIN, 19, 10, 21).plannedSendAt > s.plannedSendAt);
    }
    @Test public void materialChangeIsAMinuteOrNoneness() {
        PreconditioningSchedule a = PreconditioningSchedule.decode("1|ARRIVE_BY|100000000|0|10000000|9|3|x");
        PreconditioningSchedule b = PreconditioningSchedule.decode("1|ARRIVE_BY|100000000|0|10059000|9|3|x");
        PreconditioningSchedule c = PreconditioningSchedule.decode("1|ARRIVE_BY|100000000|0|10060000|9|3|x");
        PreconditioningSchedule none = PreconditioningSchedule.decode("1|ARRIVE_BY|100000000|0|-1|9|3|x");
        assertFalse(a.materiallyDifferent(b));
        assertTrue(a.materiallyDifferent(c));
        assertTrue(a.materiallyDifferent(none));
        assertTrue(a.materiallyDifferent(null));
    }
    @Test public void encodesAndDecodesBothModes() {
        PreconditioningSchedule start = PreconditioningSchedule.startBy(T, NOW);
        PreconditioningSchedule back = PreconditioningSchedule.decode(start.encode());
        assertEquals(start.encode(), back.encode());
        assertNull(back.minimumC);
        PreconditioningSchedule arrive = PreconditioningSchedule.arriveBy(T, NOW, -3, -12, 21);
        back = PreconditioningSchedule.decode(arrive.encode());
        assertEquals(PreconditioningSchedule.Mode.ARRIVE_BY, back.mode);
        assertEquals(arrive.plannedSendAt, back.plannedSendAt);
        assertEquals(Integer.valueOf(-3), back.minimumC);
        assertEquals(Integer.valueOf(-12), back.outsideC);
        assertEquals(arrive.reason, back.reason);
    }
    @Test public void reasonMayContainPipes() {
        String stored = "1|START_BY|100|50|30|-|-|a|b";
        PreconditioningSchedule s = PreconditioningSchedule.decode(stored);
        assertEquals("a|b", s.reason);
        assertEquals(stored, s.encode());
    }
    @Test public void malformedValuesDecodeToNull() {
        assertNull(PreconditioningSchedule.decode(null));
        assertNull(PreconditioningSchedule.decode(""));
        assertNull(PreconditioningSchedule.decode("2|START_BY|100|50|30|-|-|r"));
        assertNull(PreconditioningSchedule.decode("1|LATER|100|50|30|-|-|r"));
        assertNull(PreconditioningSchedule.decode("1|START_BY|soon|50|30|-|-|r"));
        assertNull(PreconditioningSchedule.decode("1|START_BY|100|50|30|-|-"));
        assertNull(PreconditioningSchedule.decode("1|START_BY|100|50|30|x|-|r"));
    }
}
