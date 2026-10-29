package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class TemperatureHistoryTest {
    @Test public void emptyHistoryHasNoSamplesOrSession() {
        TemperatureHistory.Trace trace = new TemperatureHistory().copy(1000);
        assertTrue(trace.samples.isEmpty()); assertNull(trace.latest()); assertEquals(-1, trace.sessionStart);
    }
    @Test public void ringKeepsTheNewestCapacitySamplesInOrder() {
        TemperatureHistory history = new TemperatureHistory();
        int total = TemperatureHistory.CAPACITY + 25;
        for (int i = 0; i < total; i++) history.add(1000 + i * 1000L, i % 40, null);
        TemperatureHistory.Trace trace = history.copy(1000 + total * 1000L);
        assertEquals(TemperatureHistory.CAPACITY, trace.samples.size());
        assertEquals(1000 + 25 * 1000L, trace.samples.get(0).at);
        assertEquals(1000 + (total - 1) * 1000L, trace.latest().at);
        for (int i = 1; i < trace.samples.size(); i++) assertTrue(trace.samples.get(i).at > trace.samples.get(i - 1).at);
    }
    @Test public void copyDropsSamplesOlderThanTheWindow() {
        TemperatureHistory history = new TemperatureHistory();
        history.add(1000, 10, null); history.add(1000 + TemperatureHistory.WINDOW_MS, 12, null);
        TemperatureHistory.Trace trace = history.copy(1001 + TemperatureHistory.WINDOW_MS);
        assertEquals(1, trace.samples.size()); assertEquals(12, trace.latest().minimumC);
        assertEquals(2, history.copy(1000 + TemperatureHistory.WINDOW_MS).samples.size());
    }
    @Test public void copiesAreImmutableSnapshots() {
        TemperatureHistory history = new TemperatureHistory();
        history.add(1000, 10, null);
        TemperatureHistory.Trace trace = history.copy(1000);
        history.add(2000, 11, null); history.markSessionStart(2000);
        assertEquals(1, trace.samples.size()); assertEquals(-1, trace.sessionStart);
        try { trace.samples.clear(); fail(); } catch (UnsupportedOperationException expected) { }
    }
    @Test public void outOfOrderSamplesAndInvertedMaximumAreRejected() {
        TemperatureHistory history = new TemperatureHistory();
        history.add(5000, 10, 14); history.add(5000, 11, null); history.add(4000, 12, null);
        history.add(6000, 15, 9);
        TemperatureHistory.Trace trace = history.copy(6000);
        assertEquals(2, trace.samples.size());
        assertEquals(Integer.valueOf(14), trace.samples.get(0).maximumC);
        assertNull(trace.latest().maximumC);
    }
    @Test public void sessionStartIsReportedOnlyInsideTheWindow() {
        TemperatureHistory history = new TemperatureHistory();
        history.markSessionStart(10_000);
        assertEquals(10_000, history.copy(20_000).sessionStart);
        assertEquals(-1, history.copy(10_001 + TemperatureHistory.WINDOW_MS).sessionStart);
    }
}
