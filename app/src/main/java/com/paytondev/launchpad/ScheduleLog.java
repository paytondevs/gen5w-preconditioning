package com.paytondev.launchpad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** What the scheduler decided and did, newest last. Persisted so it outlives the process. */
final class ScheduleLog {
    static final int CAPACITY = 24;
    enum Kind {
        ARMED, REPLANNED, DEFERRED, FIRED, SKIPPED, MISSED, CANCELLED, RESTORED, RECOVERED, REJECTED;
        String label() { return name().toLowerCase(java.util.Locale.US); }
        /** A final result the user should see after coming back. */
        boolean outcome() { return this == FIRED || this == SKIPPED || this == MISSED || this == REJECTED; }
    }
    static final class Event {
        final long at;
        final Kind kind;
        final String text;
        Event(long at, Kind kind, String text) { this.at = at; this.kind = kind; this.text = text; }
    }
    static final ScheduleLog EMPTY = new ScheduleLog(Collections.<Event>emptyList());
    /** Oldest first. */
    final List<Event> events;

    private ScheduleLog(List<Event> events) { this.events = Collections.unmodifiableList(events); }

    ScheduleLog add(long at, Kind kind, String text) {
        List<Event> next = new ArrayList<>(events);
        next.add(new Event(at, kind, clean(text)));
        while (next.size() > CAPACITY) next.remove(0);
        return new ScheduleLog(next);
    }
    Event latest() { return events.isEmpty() ? null : events.get(events.size() - 1); }
    /** Up to n events, newest first. */
    List<Event> latest(int n) {
        List<Event> out = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && out.size() < n; i--) out.add(events.get(i));
        return out;
    }
    boolean lastIs(Kind kind, String text) {
        Event last = latest();
        return last != null && last.kind == kind && last.text.equals(clean(text));
    }

    String encode() {
        StringBuilder out = new StringBuilder();
        for (Event e : events) {
            if (out.length() > 0) out.append('\n');
            out.append(e.at).append('|').append(e.kind.name()).append('|').append(e.text);
        }
        return out.toString();
    }
    static ScheduleLog decode(String value) {
        if (value == null || value.isEmpty()) return EMPTY;
        List<Event> events = new ArrayList<>();
        for (String line : value.split("\n")) {
            String[] parts = line.split("\\|", 3);
            if (parts.length != 3) continue;
            try { events.add(new Event(Long.parseLong(parts[0]), Kind.valueOf(parts[1]), parts[2])); }
            catch (IllegalArgumentException ignored) { }
        }
        while (events.size() > CAPACITY) events.remove(0);
        return new ScheduleLog(events);
    }
    private static String clean(String text) {
        return text == null ? "" : text.replace('\r', ' ').replace('\n', ' ');
    }
}
