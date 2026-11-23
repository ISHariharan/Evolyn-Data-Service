package com.evolyn.evolyn_data_service.Goals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class GoalMathTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate END = LocalDate.of(2026, 11, 30);

    private static GoalMath.Progress at(String total, LocalDate today) {
        return GoalMath.progress(new BigDecimal("60000"), new BigDecimal(total), START, END, today);
    }

    @Test
    void midMonthMatchesTheDashboardExample() {
        // 41,280 kcal by 23 Nov: 68.8%, 18,720 left over 8 days = 2,340/day, average 1,794.78, pace 53,843
        GoalMath.Progress p = at("41280", LocalDate.of(2026, 11, 23));
        assertThat(p.phase()).isEqualTo(GoalMath.Phase.ACTIVE);
        assertThat(p.percent()).isEqualByComparingTo("68.8");
        assertThat(p.remaining()).isEqualByComparingTo("18720");
        assertThat(p.elapsedDays()).isEqualTo(23);
        assertThat(p.remainingDays()).isEqualTo(8);
        assertThat(p.neededPerDay()).isEqualByComparingTo("2340.00");
        assertThat(p.averagePerDay()).isEqualByComparingTo("1794.78");
        assertThat(p.projectedTotal()).isEqualByComparingTo("53843");
        assertThat(p.projectedPercent()).isEqualByComparingTo("89.7");
        assertThat(p.reached()).isFalse();
    }

    @Test
    void firstAndLastDay() {
        GoalMath.Progress first = at("0", START);
        assertThat(first.elapsedDays()).isEqualTo(1);
        assertThat(first.remainingDays()).isEqualTo(30);
        assertThat(first.neededPerDay()).isEqualByComparingTo("2000.00");

        GoalMath.Progress last = at("58000", END);
        assertThat(last.remainingDays()).isEqualTo(1);
        assertThat(last.neededPerDay()).isEqualByComparingTo("2000.00");
    }

    @Test
    void beforeTheMonthStarts() {
        GoalMath.Progress p = at("0", LocalDate.of(2026, 10, 25));
        assertThat(p.phase()).isEqualTo(GoalMath.Phase.UPCOMING);
        assertThat(p.averagePerDay()).isNull();
        assertThat(p.projectedTotal()).isNull();
        assertThat(p.neededPerDay()).isEqualByComparingTo("2000.00");
    }

    @Test
    void afterTheMonthEnds() {
        GoalMath.Progress p = at("61000", LocalDate.of(2026, 12, 3));
        assertThat(p.phase()).isEqualTo(GoalMath.Phase.ENDED);
        assertThat(p.remainingDays()).isZero();
        assertThat(p.neededPerDay()).isNull();
        assertThat(p.projectedTotal()).isNull();
        assertThat(p.reached()).isTrue();
        assertThat(p.remaining()).isEqualByComparingTo("0");
        assertThat(p.percent()).isEqualByComparingTo("101.7");
    }

    @Test
    void nothingNeededOnceReached() {
        assertThat(at("60000", LocalDate.of(2026, 11, 20)).neededPerDay()).isNull();
    }

    @Test
    void rejectsBadTargets() {
        assertThatThrownBy(() -> GoalMath.progress(BigDecimal.ZERO, BigDecimal.ONE, START, END, START))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tiesShareARank() {
        record P(String name, BigDecimal kcal) {}
        List<GoalMath.Ranked<P>> r = GoalMath.rank(List.of(
                new P("c", new BigDecimal("700")), new P("a", new BigDecimal("900")),
                new P("d", new BigDecimal("500")), new P("b", new BigDecimal("700"))), P::kcal);
        assertThat(r).extracting(GoalMath.Ranked::rank).containsExactly(1, 2, 2, 4);
        assertThat(r.get(0).item().name()).isEqualTo("a");
    }
}
