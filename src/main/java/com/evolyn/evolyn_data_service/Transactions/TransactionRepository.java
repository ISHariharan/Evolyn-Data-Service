package com.evolyn.evolyn_data_service.Transactions;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * JpaSpecificationExecutor lets us build the WHERE clause at runtime
 * (see TransactionSpecs) so every filter can be optional.
 */
@Repository
public interface TransactionRepository
        extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

    // @EntityGraph = also fetch the account in the same SQL query (a JOIN),
    // instead of one extra query per row (the "N+1 query" problem).
    @Override
    @EntityGraph(attributePaths = { "account", "toAccount", "category" })
    Page<TransactionEntity> findAll(Specification<TransactionEntity> spec, Pageable pageable);

    @EntityGraph(attributePaths = { "account", "toAccount", "category" })
    Optional<TransactionEntity> findByTransactionIdAndUserId(UUID transactionId, UUID userId);

    long countByAccount_AccountId(UUID accountId);

    long countByToAccount_AccountId(UUID accountId);
}
