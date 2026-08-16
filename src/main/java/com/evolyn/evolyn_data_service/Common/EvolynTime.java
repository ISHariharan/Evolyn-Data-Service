package com.evolyn.evolyn_data_service.Common;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Everyone using Evolyn is in India, so "today", streak days and monthly goals
 * all use India time. Timestamps are still stored in UTC; this zone only decides
 * where a day starts and ends.
 */
public final class EvolynTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private EvolynTime() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
