package com.evolyn.evolyn_data_service.Accounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AccountRequest(
        UUID userId,
        String name,
        String accountType,
        String last4,
        BigDecimal openingBalance,      // optional; requires openingBalanceDate
        LocalDate openingBalanceDate,
        BigDecimal creditLimit,         // credit cards only
        Integer statementDay) {         // credit cards only, 1-28
}
