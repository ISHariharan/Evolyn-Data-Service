package com.evolyn.evolyn_data_service.Transactions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The JSON body Evolyn-API sends when creating or updating a transaction.
 * Field validation already happened in Evolyn-API; this service checks
 * only what needs the database (e.g. "does this account belong to this user?").
 */
public record TransactionRequest(
        UUID userId,
        UUID accountId,
        String type,
        BigDecimal amount,
        LocalDate transactionDate,
        UUID categoryId,
        String merchant,
        String notes,
        String paymentMethod) {
}
