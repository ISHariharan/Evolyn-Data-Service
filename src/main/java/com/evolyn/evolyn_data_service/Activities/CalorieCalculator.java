package com.evolyn.evolyn_data_service.Activities;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estimated calories from the Compendium's MET values.
 *
 *   kcal = MET x body weight (kg) x duration (hours)       ("gross" method)
 *
 * Example: team handball (MET 8.0), 72.4 kg, 90 minutes -> 8.0 x 72.4 x 1.5 = 868.80 kcal.
 *
 * This is an estimate, not a measurement. The formula version is saved with
 * every calculation, so switching method later (e.g. net = (MET - 1) x kg x h)
 * would be a new version, never a silent change to old numbers.
 */
public final class CalorieCalculator {

    public static final String FORMULA_VERSION = "MET_GROSS_V1";
    /** Calories the person entered themselves (e.g. from a watch), instead of the estimate. */
    public static final String ENTERED_VERSION = "ENTERED_V1";
    public static final int MAX_ENTERED_KCAL = 5000;
    public static final String METRIC = "CALORIES_BURNED";
    public static final String UNIT = "kcal";

    private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);

    private CalorieCalculator() {}

    public static BigDecimal kcal(BigDecimal met, BigDecimal weightKg, int durationSeconds) {
        if (met == null || met.signum() <= 0) throw new IllegalArgumentException("MET must be positive");
        if (weightKg == null || weightKg.signum() <= 0) throw new IllegalArgumentException("Weight must be positive");
        if (durationSeconds <= 0) throw new IllegalArgumentException("Duration must be positive");
        // Multiply first, divide once at the end, so no precision is lost on the way
        return met.multiply(weightKg)
                .multiply(BigDecimal.valueOf(durationSeconds))
                .divide(SECONDS_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}
