package com.paytondev.launchpad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Display-only trace of successful reads. Gaps stay gaps; nothing is interpolated. */
final class TemperatureHistory {
    static final int CAPACITY = 180;
    static final long WINDOW_MS = 30 * 60 * 1000L;
    /** Longer than three missed polls: draw a break instead of a line. */
    static final long GAP_MS = 3 * BatteryTemperatureReading.REFRESH_MS + 5000;
    static final class Sample {
        final long at;
        final int minimumC;
        final Integer maximumC;
        Sample(long at, int minimumC, Integer maximumC) { this.at = at; this.minimumC = minimumC; this.maximumC = maximumC; }
    }
    /** Immutable copy for the UI thread. */
    static final class Trace {
        static final Trace EMPTY = new Trace(Collections.<Sample>emptyList(), -1);
        final List<Sample> samples;
        final long sessionStart;
        Trace(List<Sample> samples, long sessionStart) { this.samples = samples; this.sessionStart = sessionStart; }
        Sample latest() { return samples.isEmpty() ? null : samples.get(samples.size() - 1); }
    }
    private final Sample[] ring = new Sample[CAPACITY];
    private int head, size;
    private long sessionStart = -1;

    synchronized void add(long at, int minimumC, Integer maximumC) {
        // elapsedRealtime never runs backwards in one process; reject anything that does.
        if (size > 0 && at <= ring[(head + size - 1) % CAPACITY].at) return;
        if (maximumC != null && maximumC < minimumC) maximumC = null;
        ring[(head + size) % CAPACITY] = new Sample(at, minimumC, maximumC);
        if (size < CAPACITY) size++; else head = (head + 1) % CAPACITY;
    }
    synchronized void markSessionStart(long at) { sessionStart = at; }
    synchronized Trace copy(long now) {
        List<Sample> samples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Sample sample = ring[(head + i) % CAPACITY];
            if (now - sample.at <= WINDOW_MS) samples.add(sample);
        }
        return new Trace(Collections.unmodifiableList(samples), sessionStart >= 0 && now - sessionStart <= WINDOW_MS ? sessionStart : -1);
    }
}
