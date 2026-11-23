package com.evolyn.evolyn_data_service.Activities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/** kcal = MET x weight (kg) x hours, kept to 2 decimals. */
class CalorieCalculatorTest {

    private static BigDecimal kcal(String met, String kg, int seconds) {
        return CalorieCalculator.kcal(new BigDecimal(met), new BigDecimal(kg), seconds);
    }

    @Test
    void teamHandballNinetyMinutes() {
        // 8.0 x 72.4 x 1.5 = 868.80
        assertThat(kcal("8.0", "72.4", 90 * 60)).isEqualByComparingTo("868.80");
    }

    @Test
    void specExample() {
        // From the Tribe spec: MET 8, 80 kg, 30 minutes = 320 kcal
        assertThat(kcal("8.0", "80", 30 * 60)).isEqualByComparingTo("320.00");
    }

    @Test
    void oddDurationsRoundOnceAtTheEnd() {
        // 7.5 x 72.9 x (1800 / 3600) = 273.375 -> 273.38
        assertThat(kcal("7.5", "72.9", 1800)).isEqualByComparingTo("273.38");
        // 1 second still counts: 8.0 x 70 / 3600 = 0.1555... -> 0.16
        assertThat(kcal("8.0", "70", 1)).isEqualByComparingTo("0.16");
    }

    @Test
    void resultAlwaysHasTwoDecimals() {
        assertThat(kcal("6.0", "70", 3600).scale()).isEqualTo(2);
    }

    @Test
    void refusesMissingOrNonPositiveInputs() {
        assertThatThrownBy(() -> kcal("0", "70", 60)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kcal("8.0", "-1", 60)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> kcal("8.0", "70", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CalorieCalculator.kcal(null, BigDecimal.TEN, 60)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formulaVersionIsGross() {
        assertThat(CalorieCalculator.FORMULA_VERSION).isEqualTo("MET_GROSS_V1");
    }
}
