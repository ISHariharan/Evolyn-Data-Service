package com.evolyn.evolyn_data_service.Streaks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class StreakMathTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    private static LocalDate d(int day) {
        return LocalDate.of(2026, 11, day);
    }

    @Test
    void runEndingToday() {
        StreakMath.Streak s = StreakMath.of(List.of(d(20), d(21), d(22), d(23)), TODAY);
        assertThat(s.current()).isEqualTo(4);
        assertThat(s.activeToday()).isTrue();
        assertThat(s.atRisk()).isFalse();
    }

    @Test
    void notActiveYetTodayKeepsYesterdaysRunAtRisk() {
        StreakMath.Streak s = StreakMath.of(List.of(d(20), d(21), d(22)), TODAY);
        assertThat(s.current()).isEqualTo(3);
        assertThat(s.activeToday()).isFalse();
        assertThat(s.atRisk()).isTrue();
    }

    @Test
    void aMissedDayBreaksIt() {
        StreakMath.Streak s = StreakMath.of(List.of(d(18), d(19), d(20), d(21)), TODAY); // 22 missed
        assertThat(s.current()).isZero();
        assertThat(s.atRisk()).isFalse();
        assertThat(s.best()).isEqualTo(4);
        assertThat(s.lastActive()).isEqualTo(d(21));
    }

    @Test
    void bestIsTheLongestRunEver() {
        StreakMath.Streak s = StreakMath.of(List.of(d(1), d(2), d(3), d(4), d(5), d(10), d(22), d(23)), TODAY);
        assertThat(s.best()).isEqualTo(5);
        assertThat(s.current()).isEqualTo(2);
    }

    @Test
    void acrossMonthEnds() {
        StreakMath.Streak s = StreakMath.of(List.of(LocalDate.of(2026, 10, 31), d(1), d(2)), d(2));
        assertThat(s.current()).isEqualTo(3);
    }

    @Test
    void duplicatesAndFutureDaysDontCount() {
        StreakMath.Streak s = StreakMath.of(List.of(d(22), d(22), d(23), d(24), d(25)), TODAY);
        assertThat(s.current()).isEqualTo(2);
        assertThat(s.best()).isEqualTo(2);
    }

    @Test
    void nothingYet() {
        StreakMath.Streak s = StreakMath.of(List.of(), TODAY);
        assertThat(s.current()).isZero();
        assertThat(s.best()).isZero();
        assertThat(s.lastActive()).isNull();
    }

    @Test
    void membersNeededRoundsUp() {
        assertThat(StreakMath.needed(8, 50)).isEqualTo(4);
        assertThat(StreakMath.needed(5, 50)).isEqualTo(3);
        assertThat(StreakMath.needed(1, 50)).isEqualTo(1);
        assertThat(StreakMath.needed(3, 100)).isEqualTo(3);
        assertThat(StreakMath.needed(10, 1)).isEqualTo(1);
    }

    @Test
    void tribeDaysNeedEnoughMembers() {
        assertThat(StreakMath.tribeDays(Map.of(d(21), 4, d(22), 3, d(23), 5), 4))
                .containsExactlyInAnyOrder(d(21), d(23));
    }
}
