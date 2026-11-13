package com.evolyn.evolyn_data_service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.evolyn.evolyn_data_service.Transactions.TransactionView;

/** JSON shapes returned by the dashboard endpoints. */
public final class DashboardResponses {

    private DashboardResponses() {}

    public record CategoryTotal(UUID categoryId, String name, String icon, String color, BigDecimal total, long count) {}
    public record AccountTotal(UUID accountId, String name, String last4, BigDecimal total) {}
    public record MethodTotal(String paymentMethod, BigDecimal total) {}
    public record MerchantTotal(String merchant, BigDecimal total, long count) {}
    public record DayTotal(LocalDate date, BigDecimal total) {}

    /** Everything the dashboard needs for one period, in one response. */
    public record Summary(
            LocalDate from,
            LocalDate to,
            BigDecimal totalSpent,
            long expenseCount,
            BigDecimal totalIncome,
            long incomeCount,
            List<CategoryTotal> byCategory,
            List<AccountTotal> byAccount,
            List<MethodTotal> byPaymentMethod,
            List<MerchantTotal> topMerchants,
            List<TransactionView> biggestExpenses,
            List<DayTotal> dailySpending) {}

    /**
     * An account's current position.
     *  balance: for bank / cash / wallet, what's in it; for a credit card, what you owe.
     *           null when no opening balance has been set (it can't be known).
     *  cycle*:  credit cards only; the current billing cycle (or calendar month when no
     *           statement day is set) and how much was spent on the card in it so far.
     */
    public record AccountBalance(
            UUID accountId,
            String name,
            String last4,
            String accountType,
            BigDecimal openingBalance,
            LocalDate openingBalanceDate,
            BigDecimal balance,
            BigDecimal creditLimit,
            Integer statementDay,
            LocalDate cycleStart,
            LocalDate nextStatementDate,
            BigDecimal cycleSpent) {}

    /** One bar pair in the month-by-month chart. month = "2026-10". */
    public record MonthTotal(String month, BigDecimal income, BigDecimal spent) {}
}
