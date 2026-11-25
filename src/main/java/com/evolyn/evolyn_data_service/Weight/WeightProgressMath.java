package com.evolyn.evolyn_data_service.Weight;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How someone's weight changed over a period, without a database.
 *
 * The start is their first weigh-in in the period; every later weigh-in is shown as
 * the change from it, in kg (rounded to 0.1). Only changes leave this class for other
 * people, never the weights themselves.
 */
public final class WeightProgressMath {

    private WeightProgressMath() {}

    public record WeighIn(LocalDate date, BigDecimal kg) {}

    public record Point(LocalDate date, BigDecimal changeKg) {}

    public record Progress(
            LocalDate startDate, BigDecimal startKg,
            LocalDate latestDate, BigDecimal latestKg,
            BigDecimal changeKg,          // latest - start
            int weighIns,
            List<Point> points) {}

    /** null when there are no weigh-ins. */
    public static Progress progress(List<WeighIn> weighIns) {
        if (weighIns == null || weighIns.isEmpty()) return null;
        List<WeighIn> sorted = new ArrayList<>(weighIns);
        sorted.sort(Comparator.comparing(WeighIn::date));
        WeighIn first = sorted.get(0);
        WeighIn last = sorted.get(sorted.size() - 1);
        List<Point> points = sorted.stream()
                .map(w -> new Point(w.date(), round(w.kg().subtract(first.kg()))))
                .toList();
        return new Progress(first.date(), first.kg(), last.date(), last.kg(),
                round(last.kg().subtract(first.kg())), sorted.size(), points);
    }

    static BigDecimal round(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP);
    }
}
