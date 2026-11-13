package com.evolyn.evolyn_data_service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.evolyn.evolyn_data_service.Transactions.TransactionEntity;

/**
 * Read-only aggregate queries for the dashboard.
 * Each query lets Postgres do the maths (SUM / COUNT / GROUP BY) and returns
 * a handful of rows, instead of sending every transaction to Java or the browser.
 *
 * The small interfaces below are "projections": Spring maps each selected
 * column alias (e.g. "as total") onto the getter with the same name.
 */
public interface DashboardQueries extends Repository<TransactionEntity, UUID> {

    interface TypeTotal { String getType(); BigDecimal getTotal(); Long getCount(); }
    interface CategoryTotal { UUID getCategoryId(); String getName(); String getIcon(); String getColor(); BigDecimal getTotal(); Long getCount(); }
    interface AccountTotal { UUID getAccountId(); String getName(); String getLast4(); BigDecimal getTotal(); }
    interface MethodTotal { String getPaymentMethod(); BigDecimal getTotal(); }
    interface MerchantTotal { String getMerchant(); BigDecimal getTotal(); Long getCount(); }
    interface DayTotal { LocalDate getDay(); BigDecimal getTotal(); }
    interface MonthTotal { Integer getYear(); Integer getMonth(); String getType(); BigDecimal getTotal(); }

    // SELECT type, SUM(amount), COUNT(*) ... GROUP BY type   -> income and spending totals
    @Query("""
        select t.type as type, sum(t.amount) as total, count(t) as count
        from TransactionEntity t
        where t.userId = :userId and t.transactionDate between :from and :to
        group by t.type""")
    List<TypeTotal> totalsByType(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // JOIN to categories so the dashboard gets each category's current name, icon and colour
    @Query("""
        select c.categoryId as categoryId, c.name as name, c.icon as icon, c.color as color,
               sum(t.amount) as total, count(t) as count
        from TransactionEntity t join t.category c
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        group by c.categoryId, c.name, c.icon, c.color
        order by sum(t.amount) desc""")
    List<CategoryTotal> spendingByCategory(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // JOIN to accounts so we can show "HDFC Bank •••• 1234" instead of an id
    @Query("""
        select a.accountId as accountId, a.name as name, a.last4 as last4, sum(t.amount) as total
        from TransactionEntity t join t.account a
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        group by a.accountId, a.name, a.last4
        order by sum(t.amount) desc""")
    List<AccountTotal> spendingByAccount(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select t.paymentMethod as paymentMethod, sum(t.amount) as total
        from TransactionEntity t
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        group by t.paymentMethod
        order by sum(t.amount) desc""")
    List<MethodTotal> spendingByPaymentMethod(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    // "Where you pay most often": sorted by number of payments, then amount.
    // (Biggest single payments have their own query below, so this one shows habits.)
    // Groups "Swiggy", "swiggy" and " Swiggy " together by grouping on lower(trim(merchant)).
    // The Pageable adds LIMIT, so we only get the top N.
    @Query("""
        select min(t.merchant) as merchant, sum(t.amount) as total, count(t) as count
        from TransactionEntity t
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        group by lower(trim(t.merchant))
        order by count(t) desc, sum(t.amount) desc""")
    List<MerchantTotal> topMerchants(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable limit);

    // "join fetch" loads each row's account in the same query (no N+1)
    @Query("""
        select t from TransactionEntity t join fetch t.account left join fetch t.category
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        order by t.amount desc, t.transactionDate desc""")
    List<TransactionEntity> biggestExpenses(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable limit);

    @Query("""
        select t.transactionDate as day, sum(t.amount) as total
        from TransactionEntity t
        where t.userId = :userId and t.type = 'EXPENSE' and t.transactionDate between :from and :to
        group by t.transactionDate
        order by t.transactionDate""")
    List<DayTotal> spendingByDay(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
        select year(t.transactionDate) as year, month(t.transactionDate) as month, t.type as type, sum(t.amount) as total
        from TransactionEntity t
        where t.userId = :userId and t.transactionDate between :from and :to and t.type <> 'TRANSFER'
        group by year(t.transactionDate), month(t.transactionDate), t.type""")
    List<MonthTotal> totalsByMonth(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
