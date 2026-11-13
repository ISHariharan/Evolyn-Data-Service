package com.evolyn.evolyn_data_service.Transactions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Builds: WHERE user_id = ? [AND type = ?] [AND category_id = ?] [AND account_id = ?]
 *               [AND transaction_date >= ?] [AND transaction_date <= ?]
 * Each filter is only added when the caller supplied it.
 */
final class TransactionSpecs {

    private TransactionSpecs() {}

    static Specification<TransactionEntity> matching(
            UUID userId, String type, UUID categoryId, UUID accountId, LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            where.add(cb.equal(root.get("userId"), userId));
            if (type != null && !type.isBlank()) where.add(cb.equal(root.get("type"), type));
            if (categoryId != null) where.add(cb.equal(root.get("category").get("categoryId"), categoryId));
            if (accountId != null) where.add(cb.equal(root.get("account").get("accountId"), accountId));
            if (from != null) where.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            if (to != null) where.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            return cb.and(where.toArray(Predicate[]::new));
        };
    }
}
