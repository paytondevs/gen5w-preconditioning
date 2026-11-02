package com.paytondev.launchpad;

/**
 * A user-armed request to send Start at a wall-clock time. Immutable; the service
 * persists it as one string so a killed or rebooted head unit can restore it.
 *
 * START_BY: conditioning should begin at the target, so Start goes out SEND_LEAD_MS
 * earlier (the car takes about 60–70 s to report Active).
 * ARRIVE_BY: the pack should be warm at the target; ArriveByPlanner picks the send time
 * from the pack and outside temperatures, and the service re-plans as readings arrive.
 */
final class PreconditioningSchedule {
    enum Mode { START_BY, ARRIVE_BY }
    enum Due { WAIT, DUE, EXPIRED }
    static final long SEND_LEAD_MS = 70_000L;
    /** A send that cannot happen by target + GRACE_MS is reported missed, never sent late. */
    static final long GRACE_MS = 5 * 60_000L;
    static final long MIN_AHEAD_MS = 2 * 60_000L;
    static final long MAX_AHEAD_MS = 24 * 60 * 60_000L;
    /** Within this long of the send time an arrive-by plan may only move earlier. */
    static final long LOCK_MS = 5 * 60_000L;
    /** plannedSendAt when nothing needs sending: the pack is already warm. */
    static final long NONE = -1;
    private static final int FORMAT = 1;

    final Mode mode;
    final long targetAt, createdAt, plannedSendAt;
    /** Planner inputs at the last plan, for the log and the monitor; may be null. */
    final Integer minimumC, outsideC;
    final String reason;

    private PreconditioningSchedule(Mode mode, long targetAt, long createdAt, long plannedSendAt,
                                    Integer minimumC, Integer outsideC, String reason) {
        this.mode = mode; this.targetAt = targetAt; this.createdAt = createdAt;
        this.plannedSendAt = plannedSendAt; this.minimumC = minimumC; this.outsideC = outsideC;
        this.reason = reason == null ? "" : reason.replace('\n', ' ');
    }

    static PreconditioningSchedule startBy(long targetAt, long now) {
        return new PreconditioningSchedule(Mode.START_BY, targetAt, now, targetAt - SEND_LEAD_MS, null, null,
            "Precondition asks the car a minute early so it's running on time.");
    }
    static PreconditioningSchedule arriveBy(long targetAt, long now, Integer minimumC, Integer outsideC, int thresholdC) {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(targetAt, minimumC, outsideC, thresholdC, now);
        return new PreconditioningSchedule(Mode.ARRIVE_BY, targetAt, now, plan.sendAt, minimumC, outsideC, plan.reason);
    }
    static PreconditioningSchedule create(Mode mode, long targetAt, long now, Integer minimumC, Integer outsideC, int thresholdC) {
        return mode == Mode.ARRIVE_BY ? arriveBy(targetAt, now, minimumC, outsideC, thresholdC) : startBy(targetAt, now);
    }
    /** Earliest acceptable target: far enough ahead to send in time, at most a day out. */
    static boolean acceptable(long targetAt, long now) {
        return targetAt >= now + MIN_AHEAD_MS && targetAt <= now + MAX_AHEAD_MS;
    }

    /** A fresh arrive-by plan from new readings. Start-by never changes. */
    PreconditioningSchedule replanned(long now, Integer minimumC, Integer outsideC, int thresholdC) {
        if (mode != Mode.ARRIVE_BY) return this;
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(targetAt, minimumC, outsideC, thresholdC, now);
        // Close to the send, only move earlier: a reading that flickers must not keep
        // pushing the send back. A pack that turns out warm is caught at send time.
        boolean locked = plannedSendAt != NONE && now >= plannedSendAt - LOCK_MS;
        if (locked && (plan.sendAt == NONE || plan.sendAt >= plannedSendAt)) return this;
        return new PreconditioningSchedule(mode, targetAt, createdAt, plan.sendAt, minimumC, outsideC, plan.reason);
    }
    /** Worth logging and re-arming the alarm for. */
    boolean materiallyDifferent(PreconditioningSchedule other) {
        if (other == null) return true;
        if ((plannedSendAt == NONE) != (other.plannedSendAt == NONE)) return true;
        return Math.abs(plannedSendAt - other.plannedSendAt) >= 60_000L;
    }
    Due due(long now) {
        if (now > targetAt + GRACE_MS) return Due.EXPIRED;
        if (plannedSendAt == NONE) return now >= targetAt ? Due.EXPIRED : Due.WAIT;
        return now >= plannedSendAt ? Due.DUE : Due.WAIT;
    }
    /** Last moment a deferred send may still go out. */
    long deadline() { return targetAt + GRACE_MS; }
    String title() { return mode == Mode.ARRIVE_BY ? "Arrive by" : "Start by"; }

    String encode() {
        return FORMAT + "|" + mode.name() + "|" + targetAt + "|" + createdAt + "|" + plannedSendAt + "|"
            + optional(minimumC) + "|" + optional(outsideC) + "|" + reason;
    }
    static PreconditioningSchedule decode(String value) {
        if (value == null || value.isEmpty()) return null;
        String[] parts = value.split("\\|", 8);
        if (parts.length != 8) return null;
        try {
            if (Integer.parseInt(parts[0]) != FORMAT) return null;
            return new PreconditioningSchedule(Mode.valueOf(parts[1]), Long.parseLong(parts[2]),
                Long.parseLong(parts[3]), Long.parseLong(parts[4]), parseOptional(parts[5]),
                parseOptional(parts[6]), parts[7]);
        } catch (IllegalArgumentException e) { return null; }
    }
    private static String optional(Integer value) { return value == null ? "-" : value.toString(); }
    private static Integer parseOptional(String value) { return "-".equals(value) ? null : Integer.valueOf(value); }
}
