package com.evolyn.evolyn_data_service.Goals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.evolyn.evolyn_data_service.Goals.DailyGoalMath.Change;

class DailyGoalMathTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    @Test
    void noGoalMeansZero() {
        assertEquals(0, DailyGoalMath.goalOn(List.of(), NOV_1));
        assertEquals(0, DailyGoalMath.target(List.of(), NOV_1, NOV_30));
    }

    @Test
    void aGoalSetBeforeTheMonthCoversEveryDay() {
        List<Change> c = List.of(new Change(LocalDate.of(2026, 10, 20), 400));
        assertEquals(30 * 400, DailyGoalMath.target(c, NOV_1, NOV_30));
    }

    @Test
    void settingItMidMonthOnlyCountsTheDaysLeft() {
        // Set on the 24th: 24th..30th = 7 days
        List<Change> c = List.of(new Change(LocalDate.of(2026, 11, 24), 500));
        assertEquals(0, DailyGoalMath.goalOn(c, LocalDate.of(2026, 11, 23)));
        assertEquals(500, DailyGoalMath.goalOn(c, LocalDate.of(2026, 11, 24)));
        assertEquals(7 * 500, DailyGoalMath.target(c, NOV_1, NOV_30));
    }

    @Test
    void aChangeAppliesFromItsDayAndKeepsEarlierDays() {
        // 300 from October, 600 from the 21st: 20 days x 300 + 10 days x 600
        List<Change> c = List.of(new Change(LocalDate.of(2026, 11, 21), 600), new Change(LocalDate.of(2026, 10, 1), 300));
        assertEquals(300, DailyGoalMath.goalOn(c, LocalDate.of(2026, 11, 20)));
        assertEquals(600, DailyGoalMath.goalOn(c, LocalDate.of(2026, 11, 21)));
        assertEquals(20 * 300 + 10 * 600, DailyGoalMath.target(c, NOV_1, NOV_30));
    }

    @Test
    void aFutureChangeDoesNotTouchThisMonth() {
        List<Change> c = List.of(new Change(LocalDate.of(2026, 10, 1), 300), new Change(LocalDate.of(2026, 12, 1), 900));
        assertEquals(30 * 300, DailyGoalMath.target(c, NOV_1, NOV_30));
    }
}
