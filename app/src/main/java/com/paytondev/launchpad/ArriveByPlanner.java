package com.paytondev.launchpad;

import java.util.Locale;

/**
 * Chooses when to send Start so the pack minimum reaches the warm threshold by arrival.
 *
 * ASSUMPTION, not a calibration: no measured heating rate exists for these cars. The
 * table is a conservative reading of owner reports (EV6 min temp 14 → 18 °C in 15 min;
 * "about 20–30 min to 20 °C"; a 4–7 kW heater, which is under 1 °C/min for a pack of
 * this mass before losses) and assumes heating slows as the outside air gets colder.
 * The car ends a conditioning session after about 30 minutes, so longer plans are capped.
 * Recorded sessions (TemperatureHistory) are the intended source for a learned rate.
 */
final class ArriveByPlanner {
    static final int SESSION_CAP_MIN = 30;
    static final int MARGIN_MIN = 2;
    static final int[] OUTSIDE_C = {-20, -10, 0, 10};
    static final double[] RATE_C_PER_MIN = {0.25, 0.30, 0.40, 0.50};
    /** Unknown outside temperature plans as if it were 0 °C. */
    static final int DEFAULT_OUTSIDE_C = 0;

    static final class Plan {
        final long sendAt;
        final int heatMinutes;
        final boolean alreadyWarm, capped, lateStart;
        final String reason;
        Plan(long sendAt, int heatMinutes, boolean alreadyWarm, boolean capped, boolean lateStart, String reason) {
            this.sendAt = sendAt; this.heatMinutes = heatMinutes; this.alreadyWarm = alreadyWarm;
            this.capped = capped; this.lateStart = lateStart; this.reason = reason;
        }
    }

    private ArriveByPlanner() { }

    static double rateCPerMin(Integer outsideC) {
        int c = outsideC == null ? DEFAULT_OUTSIDE_C : outsideC;
        if (c <= OUTSIDE_C[0]) return RATE_C_PER_MIN[0];
        int last = OUTSIDE_C.length - 1;
        if (c >= OUTSIDE_C[last]) return RATE_C_PER_MIN[last];
        int i = 0;
        while (c > OUTSIDE_C[i + 1]) i++;
        double t = (double) (c - OUTSIDE_C[i]) / (OUTSIDE_C[i + 1] - OUTSIDE_C[i]);
        return RATE_C_PER_MIN[i] + t * (RATE_C_PER_MIN[i + 1] - RATE_C_PER_MIN[i]);
    }
    /** Minutes of heating including the margin, before the session cap; null pack plans the cap. */
    static int uncappedHeatMinutes(Integer minimumC, Integer outsideC, int thresholdC) {
        if (minimumC == null) return SESSION_CAP_MIN;
        int need = Math.max(0, thresholdC - minimumC);
        if (need == 0) return 0;
        return (int) Math.ceil(need / rateCPerMin(outsideC) - 1e-9) + MARGIN_MIN;
    }

    static Plan plan(long arriveAt, Integer minimumC, Integer outsideC, int thresholdC, long now) {
        String warm = ScheduleFormat.temperature(thresholdC);
        if (BatteryTemperature.isWarm(minimumC, thresholdC))
            return new Plan(PreconditioningSchedule.NONE, 0, true, false, false,
                "Pack " + ScheduleFormat.temperature(minimumC) + " is already at " + warm
                    + "; nothing to send unless it cools");
        int minutes = uncappedHeatMinutes(minimumC, outsideC, thresholdC);
        boolean capped = minutes > SESSION_CAP_MIN;
        if (capped) minutes = SESSION_CAP_MIN;
        long sendAt = arriveAt - minutes * 60_000L - PreconditioningSchedule.SEND_LEAD_MS;
        boolean late = sendAt < now;
        if (late) sendAt = now;
        StringBuilder reason = new StringBuilder();
        if (minimumC == null) {
            reason.append("Pack temperature unknown: planning the full ").append(minutes).append(" min + 70 s");
        } else {
            reason.append("Pack ").append(ScheduleFormat.temperature(minimumC)).append(" to ")
                .append(BatteryTemperatureReading.fahrenheit(thresholdC)).append("°F at ~")
                .append(String.format(Locale.US, "%.2f", rateCPerMin(outsideC))).append("°C/min (outside ")
                .append(outsideC == null ? "unknown" : ScheduleFormat.temperature(outsideC)).append("): ")
                .append(minutes).append(" min of heat + 70 s");
        }
        if (capped) reason.append(" · capped at the car's 30 min session, may not be warm by arrival");
        if (late) reason.append(" · not enough time left, sending now");
        return new Plan(sendAt, minutes, false, capped, late, reason.toString());
    }
}
