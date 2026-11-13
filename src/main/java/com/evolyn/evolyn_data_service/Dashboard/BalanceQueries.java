package com.evolyn.evolyn_data_service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.evolyn.evolyn_data_service.Transactions.TransactionEntity;

/**
 * Sums used to work out account balances. Only transactions dated on or after an
 * account's opening-balance date count: anything earlier is already inside the
 * opening balance the user typed in.
 */
public interface BalanceQueries extends Repository<TransactionEntity, UUID> {

    interface Flow { UUID getAccountId(); String getType(); BigDecimal getTotal(); }
    interface Inflow { UUID getAccountId(); BigDecimal getTotal(); }

    // Money out of / into each account by type: EXPENSE, INCOME, and TRANSFER (= sent out)
    @Query("""
        select a.accountId as accountId, t.type as type, sum(t.amount) as total
        from TransactionEntity t join t.account a
        where t.userId = :userId and a.openingBalanceDate is not null
          and t.transactionDate >= a.openingBalanceDate
        group by a.accountId, t.type""")
    List<Flow> flowsByAccount(@Param("userId") UUID userId);

    // Transfers received by each account (e.g. a card bill payment arriving at the card)
    @Query("""
        select a.accountId as accountId, sum(t.amount) as total
        from TransactionEntity t join t.toAccount a
        where t.userId = :userId and t.type = 'TRANSFER' and a.openingBalanceDate is not null
          and t.transactionDate >= a.openingBalanceDate
        group by a.accountId""")
    List<Inflow> transfersIn(@Param("userId") UUID userId);

    // Purchases on one card between two dates (its billing cycle so far)
    @Query("""
        select coalesce(sum(t.amount), 0) from TransactionEntity t
        where t.account.accountId = :accountId and t.type = 'EXPENSE'
          and t.transactionDate between :from and :to""")
    BigDecimal spentOn(@Param("accountId") UUID accountId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
