package com.evolyn.evolyn_data_service.Challenges;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.evolyn.evolyn_data_service.Challenges.ChallengeMath.Phase;
import com.evolyn.evolyn_data_service.Challenges.ChallengeMath.Tally;
import com.evolyn.evolyn_data_service.Challenges.ChallengeMath.Type;

class ChallengeMathTest {

    private static final LocalDate START = LocalDate.of(2026, 11, 16);
    private static final LocalDate END = LocalDate.of(2026, 11, 29);
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3), D = new UUID(0, 4);

    private static Tally t(UUID u, String team, String kcal, int days) {
        return new Tally(u, team, new BigDecimal(kcal), days);
    }

    @Test
    void phasesFollowTheDates() {
        assertThat(ChallengeMath.phase("DRAFT", START, END, START, false)).isEqualTo(Phase.DRAFT);
        assertThat(ChallengeMath.phase("CANCELLED", START, END, START, false)).isEqualTo(Phase.CANCELLED);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, START.minusDays(1), false)).isEqualTo(Phase.UPCOMING);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, START, false)).isEqualTo(Phase.ACTIVE);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, END, false)).isEqualTo(Phase.ACTIVE);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, END.plusDays(1), false)).isEqualTo(Phase.UNDER_REVIEW);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, END.plusDays(2), false)).isEqualTo(Phase.COMPLETED);
        assertThat(ChallengeMath.phase("PUBLISHED", START, END, START, true)).isEqualTo(Phase.COMPLETED);
    }

    @Test
    void competitiveRanksByKcalWithSharedTies() {
        var s = ChallengeMath.score(Type.COMPETITIVE, null,
                List.of(t(A, null, "3480", 5), t(B, null, "3940", 6), t(C, null, "3480", 4), t(D, null, "100", 1)));
        assertThat(s.people()).extracting(ChallengeMath.PersonResult::userId).containsExactly(B, A, C, D);
        assertThat(s.people()).extracting(ChallengeMath.PersonResult::rank).containsExactly(1, 2, 2, 4);
        assertThat(s.total()).isEqualByComparingTo("11000");
    }

    @Test
    void collectiveAddsUpToOneTarget() {
        var s = ChallengeMath.score(Type.COLLECTIVE, new BigDecimal("10000"),
                List.of(t(A, null, "6000", 3), t(B, null, "4500", 2)));
        assertThat(s.total()).isEqualByComparingTo("10500");
        assertThat(s.totalPercent()).isEqualByComparingTo("105.0");
        assertThat(s.completed()).isTrue();
        assertThat(s.people().get(0).percent()).isEqualByComparingTo("57.1"); // A's share of the total
    }

    @Test
    void individualTargetIsDoneWhenReached() {
        var s = ChallengeMath.score(Type.INDIVIDUAL_TARGET, new BigDecimal("5000"),
                List.of(t(A, null, "5200", 6), t(B, null, "2500", 3)));
        assertThat(s.people().get(0).completed()).isTrue();
        assertThat(s.people().get(1).completed()).isFalse();
        assertThat(s.people().get(1).percent()).isEqualByComparingTo("50.0");
    }

    @Test
    void consistencyCountsDaysNotCalories() {
        var s = ChallengeMath.score(Type.CONSISTENCY, new BigDecimal("5"),
                List.of(t(A, null, "9000", 3), t(B, null, "800", 5)));
        assertThat(s.people().get(0).userId()).isEqualTo(B); // 5 days beats 3 days, whatever the kcal
        assertThat(s.people().get(0).completed()).isTrue();
        assertThat(s.people().get(1).value()).isEqualByComparingTo("3");
    }

    @Test
    void teamTotalsDecide() {
        var s = ChallengeMath.score(Type.TEAM, null,
                List.of(t(A, "A", "3000", 3), t(B, "A", "1000", 1), t(C, "B", "3500", 4), t(D, "B", "900", 1)));
        // A = 4,000, B = 4,400
        assertThat(s.teams()).extracting(ChallengeMath.TeamResult::team).containsExactly("B", "A");
        assertThat(s.teams().get(0).value()).isEqualByComparingTo("4400");
        assertThat(s.teams().get(0).won()).isTrue();
        assertThat(s.teams().get(1).members()).isEqualTo(2);
    }

    @Test
    void aTeamTieHasNoWinner() {
        var s = ChallengeMath.score(Type.TEAM, null, List.of(t(A, "A", "1000", 1), t(C, "B", "1000", 1)));
        assertThat(s.teams()).allSatisfy(team -> assertThat(team.won()).isFalse());
        assertThat(s.teams()).extracting(ChallengeMath.TeamResult::rank).containsExactly(1, 1);
    }

    @Test
    void nobodyYet() {
        var s = ChallengeMath.score(Type.COLLECTIVE, new BigDecimal("1000"), List.of());
        assertThat(s.total()).isEqualByComparingTo("0");
        assertThat(s.completed()).isFalse();
        assertThat(s.people()).isEmpty();
    }
}
