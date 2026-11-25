package com.evolyn.evolyn_data_service.Weight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.evolyn.evolyn_data_service.Weight.WeightProgressMath.WeighIn;

class WeightProgressMathTest {

    private static WeighIn w(int day, String kg) {
        return new WeighIn(LocalDate.of(2026, 11, day), new BigDecimal(kg));
    }

    @Test
    void noWeighInsMeansNoProgress() {
        assertNull(WeightProgressMath.progress(List.of()));
    }

    @Test
    void changesAreFromTheFirstWeighInWhateverTheOrderGiven() {
        var p = WeightProgressMath.progress(List.of(w(10, "71.15"), w(2, "72.40"), w(20, "70.60")));
        assertEquals(LocalDate.of(2026, 11, 2), p.startDate());
        assertEquals(LocalDate.of(2026, 11, 20), p.latestDate());
        assertEquals(new BigDecimal("-1.8"), p.changeKg());
        assertEquals(3, p.weighIns());
        assertEquals(new BigDecimal("0.0"), p.points().get(0).changeKg());
        assertEquals(new BigDecimal("-1.3"), p.points().get(1).changeKg()); // -1.25 rounds away from zero
        assertEquals(new BigDecimal("-1.8"), p.points().get(2).changeKg());
    }

    @Test
    void oneWeighInIsNoChangeYet() {
        var p = WeightProgressMath.progress(List.of(w(5, "80")));
        assertEquals(new BigDecimal("0.0"), p.changeKg());
        assertEquals(1, p.points().size());
    }

    @Test
    void gainsArePositive() {
        assertEquals(new BigDecimal("0.6"), WeightProgressMath.progress(List.of(w(1, "60.0"), w(30, "60.6"))).changeKg());
    }
}
