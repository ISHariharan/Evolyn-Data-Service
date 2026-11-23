package com.evolyn.evolyn_data_service.Profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfileIdsTest {

    @Test
    void normalizesCaseSpacesAndAt() {
        assertThat(ProfileIds.normalize("  @Arun_K ")).isEqualTo("arun_k");
        assertThat(ProfileIds.normalize(null)).isNull();
    }

    @Test
    void acceptsValidIds() {
        assertThat(ProfileIds.problem("arun_k")).isNull();
        assertThat(ProfileIds.problem("abc")).isNull();
        assertThat(ProfileIds.problem("player_10_handball_x")).isNull(); // 20 characters
    }

    @Test
    void rejectsBadLengthCharactersAndReservedWords() {
        assertThat(ProfileIds.problem("")).isNotNull();
        assertThat(ProfileIds.problem("ab")).contains("3 to 20");
        assertThat(ProfileIds.problem("player_10_handball_xy")).contains("3 to 20");
        assertThat(ProfileIds.problem("arun.k")).contains("lowercase");
        assertThat(ProfileIds.problem("arun k")).contains("lowercase");
        assertThat(ProfileIds.problem("admin")).contains("reserved");
        assertThat(ProfileIds.problem("evolyn")).contains("reserved");
    }
}
