package com.paytondev.launchpad;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArriveByPlannerTest {
    static final long NOW = 1793613600000L;
    static final long MIN = 60_000L;
    static final long T = NOW + 60 * MIN;

    @Test public void rateSlowsInTheColdAndInterpolates() {
        assertEquals(0.25, ArriveByPlanner.rateCPerMin(-30), 1e-9);
        assertEquals(0.25, ArriveByPlanner.rateCPerMin(-20), 1e-9);
        assertEquals(0.275, ArriveByPlanner.rateCPerMin(-15), 1e-9);
        assertEquals(0.30, ArriveByPlanner.rateCPerMin(-10), 1e-9);
        assertEquals(0.35, ArriveByPlanner.rateCPerMin(-5), 1e-9);
        assertEquals(0.40, ArriveByPlanner.rateCPerMin(0), 1e-9);
        assertEquals(0.45, ArriveByPlanner.rateCPerMin(5), 1e-9);
        assertEquals(0.50, ArriveByPlanner.rateCPerMin(10), 1e-9);
        assertEquals(0.50, ArriveByPlanner.rateCPerMin(30), 1e-9);
        assertEquals(0.40, ArriveByPlanner.rateCPerMin(null), 1e-9);   // unknown plans as 0 °C
    }
    @Test public void heatMinutesIncludeTheMargin() {
        assertEquals(14, ArriveByPlanner.uncappedHeatMinutes(15, 10, 21));  // 6 / 0.5 = 12, + 2
        assertEquals(7, ArriveByPlanner.uncappedHeatMinutes(18, 0, 20));    // 2 / 0.4 = 5 exactly, + 2
        assertEquals(12, ArriveByPlanner.uncappedHeatMinutes(17, 3, 21));   // 4 / 0.43 = 9.3 → 10, + 2
        assertEquals(32, ArriveByPlanner.uncappedHeatMinutes(9, 0, 21));    // 12 / 0.4 = 30, + 2
        assertEquals(30, ArriveByPlanner.uncappedHeatMinutes(null, 0, 21)); // unknown pack: the full session
        assertEquals(0, ArriveByPlanner.uncappedHeatMinutes(21, 0, 21));
    }
    @Test public void plansBackFromArrivalWithTheActivationLead() {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(T, 15, 10, 21, NOW);
        assertEquals(T - 14 * MIN - 70_000L, plan.sendAt);
        assertEquals(14, plan.heatMinutes);
        assertFalse(plan.alreadyWarm || plan.capped || plan.lateStart);
        assertTrue(plan.reason, plan.reason.contains("14 min of heat + 70 s"));
        assertTrue(plan.reason, plan.reason.contains("59°F (15°C)"));
    }
    @Test public void capsAtTheCarsThirtyMinuteSession() {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(T, 9, 0, 21, NOW);
        assertEquals(T - 30 * MIN - 70_000L, plan.sendAt);
        assertTrue(plan.capped);
        assertTrue(plan.reason, plan.reason.contains("may not be warm by arrival"));
        plan = ArriveByPlanner.plan(T, -15, -20, 21, NOW);
        assertEquals(30, plan.heatMinutes);
        assertTrue(plan.capped);
    }
    @Test public void unknownPackPlansTheFullSession() {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(T, null, null, 21, NOW);
        assertEquals(T - 30 * MIN - 70_000L, plan.sendAt);
        assertFalse(plan.capped);
        assertTrue(plan.reason, plan.reason.contains("unknown"));
    }
    @Test public void tooLateSendsNow() {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(NOW + 5 * MIN, 15, 10, 21, NOW);
        assertEquals(NOW, plan.sendAt);
        assertTrue(plan.lateStart);
        assertTrue(plan.reason, plan.reason.contains("sending now"));
    }
    @Test public void warmPackSendsNothing() {
        ArriveByPlanner.Plan plan = ArriveByPlanner.plan(T, 21, 0, 21, NOW);
        assertTrue(plan.alreadyWarm);
        assertEquals(PreconditioningSchedule.NONE, plan.sendAt);
        assertTrue(plan.reason, plan.reason.contains("already"));
        assertFalse(ArriveByPlanner.plan(T, 20, 0, 21, NOW).alreadyWarm);
    }
}
