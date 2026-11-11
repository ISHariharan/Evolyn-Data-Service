package com.evolyn.evolyn_data_service.Transactions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What we send back as JSON. We never return the JPA entity directly:
 * the entity has a lazy link to AccountEntity, and a View lets us choose
 * exactly which fields leave the service (here: the account's name and last 4).
 */
public record TransactionView(
        UUID transactionId,
        String type,
        BigDecimal amount,
        LocalDate transactionDate,
        String category,
        String merchant,
        String notes,
        String paymentMethod,
        UUID accountId,
        String accountName,
        String accountLast4,
        String source,
        Instant createdAt,
        Instant updatedAt) {

    public static TransactionView from(TransactionEntity t) {
        return new TransactionView(
                t.getTransactionId(), t.getType(), t.getAmount(), t.getTransactionDate(),
                t.getCategory(), t.getMerchant(), t.getNotes(), t.getPaymentMethod(),
                t.getAccount().getAccountId(), t.getAccount().getName(), t.getAccount().getLast4(),
                t.getSource(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
