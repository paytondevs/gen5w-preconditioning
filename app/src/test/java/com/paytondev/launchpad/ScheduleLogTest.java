package com.paytondev.launchpad;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ScheduleLogTest {
    @Test public void keepsTheNewestTwentyFour() {
        ScheduleLog log = ScheduleLog.EMPTY;
        for (int i = 0; i < 30; i++) log = log.add(i, ScheduleLog.Kind.REPLANNED, "event " + i);
        assertEquals(ScheduleLog.CAPACITY, log.events.size());
        assertEquals("event 6", log.events.get(0).text);
        assertEquals("event 29", log.latest().text);
        List<ScheduleLog.Event> latest = log.latest(3);
        assertEquals("event 29", latest.get(0).text);
        assertEquals("event 27", latest.get(2).text);
        assertEquals(1, ScheduleLog.EMPTY.add(1, ScheduleLog.Kind.ARMED, "x").latest(5).size());
    }
    @Test public void addingNeverChangesTheOriginal() {
        ScheduleLog empty = ScheduleLog.EMPTY;
        empty.add(1, ScheduleLog.Kind.ARMED, "x");
        assertTrue(empty.events.isEmpty());
        assertNull(empty.latest());
    }
    @Test public void lastIsMatchesKindAndText() {
        ScheduleLog log = ScheduleLog.EMPTY.add(1, ScheduleLog.Kind.DEFERRED, "Not READY");
        assertTrue(log.lastIs(ScheduleLog.Kind.DEFERRED, "Not READY"));
        assertFalse(log.lastIs(ScheduleLog.Kind.DEFERRED, "Other"));
        assertFalse(log.lastIs(ScheduleLog.Kind.MISSED, "Not READY"));
    }
    @Test public void roundTripsWithPipesAndFlattensNewlines() {
        ScheduleLog log = ScheduleLog.EMPTY.add(5, ScheduleLog.Kind.ARMED, "Start by 1:30 PM | a")
            .add(9, ScheduleLog.Kind.FIRED, "line one\nline two");
        ScheduleLog back = ScheduleLog.decode(log.encode());
        assertEquals(2, back.events.size());
        assertEquals("Start by 1:30 PM | a", back.events.get(0).text);
        assertEquals("line one line two", back.latest().text);
        assertEquals(ScheduleLog.Kind.FIRED, back.latest().kind);
        assertEquals(9, back.latest().at);
        assertEquals("", ScheduleLog.EMPTY.encode());
    }
    @Test public void skipsMalformedLines() {
        ScheduleLog log = ScheduleLog.decode("1|ARMED|ok\nbad\nx|FIRED|no\n2|NOPE|no\n3|MISSED|fine");
        assertEquals(2, log.events.size());
        assertEquals(ScheduleLog.Kind.MISSED, log.latest().kind);
        assertTrue(ScheduleLog.decode(null).events.isEmpty());
    }
    @Test public void outcomesAreTheFinalKinds() {
        assertTrue(ScheduleLog.Kind.FIRED.outcome());
        assertTrue(ScheduleLog.Kind.MISSED.outcome());
        assertFalse(ScheduleLog.Kind.DEFERRED.outcome());
        assertEquals("Updated", ScheduleLog.Kind.REPLANNED.label());
        assertEquals("Started", ScheduleLog.Kind.FIRED.label());
    }
}
