package com.evolyn.evolyn_data_service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Accounts.AccountEntity;
import com.evolyn.evolyn_data_service.Accounts.AccountRepository;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.AccountBalance;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.AccountTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.CategoryTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.DayTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.MerchantTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.MethodTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.MonthTotal;
import com.evolyn.evolyn_data_service.Dashboard.DashboardResponses.Summary;
import com.evolyn.evolyn_data_service.Transactions.TransactionView;

/**
 * Internal endpoints (only Evolyn-API calls these):
 *   GET /dashboard/summary?userId=&from=&to=   totals and breakdowns for one period
 *   GET /dashboard/trend?userId=&from=&to=     income vs spending per calendar month
 *   GET /dashboard/accounts?userId=&today=     current balance of each account
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/dashboard", produces = "application/json")
public class DashboardController {

    private static final int TOP_N = 5;

    private final DashboardQueries queries;
    private final BalanceQueries balances;
    private final AccountRepository accounts;

    public DashboardController(DashboardQueries queries, BalanceQueries balances, AccountRepository accounts) {
        this.queries = queries;
        this.balances = balances;
        this.accounts = accounts;
    }

    /**
     * Current balance of every account, plus each credit card's spend this cycle.
     * `today` comes from the browser so "today" means the user's date, not the
     * server's (Render runs on UTC, 5½ hours behind India).
     */
    @GetMapping("/accounts")
    @Transactional(readOnly = true)
    public List<AccountBalance> accountBalances(
            @RequestParam("userId") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {

        // net[account] = income - expenses - transfers out + transfers in
        Map<UUID, BigDecimal> net = new HashMap<>();
        for (var f : balances.flowsByAccount(userId)) {
            BigDecimal signed = "INCOME".equals(f.getType()) ? f.getTotal() : f.getTotal().negate();
            net.merge(f.getAccountId(), signed, BigDecimal::add);
        }
        for (var in : balances.transfersIn(userId)) {
            net.merge(in.getAccountId(), in.getTotal(), BigDecimal::add);
        }

        List<AccountBalance> result = new ArrayList<>();
        for (AccountEntity a : accounts.findByUserIdOrderByCreatedAtAsc(userId)) {
            boolean card = "CREDIT_CARD".equals(a.getAccountType());
            BigDecimal flow = net.getOrDefault(a.getAccountId(), BigDecimal.ZERO);

            // Without a starting balance, start from zero and use every transaction
            String source = a.getOpeningBalance() != null ? "OPENING_BALANCE"
                    : net.containsKey(a.getAccountId()) ? "TRANSACTIONS" : "NONE";
            BigDecimal opening = a.getOpeningBalance() == null ? BigDecimal.ZERO : a.getOpeningBalance();
            // Card: spending raises what you owe, payments and refunds lower it
            BigDecimal balance = card ? opening.subtract(flow) : opening.add(flow);

            LocalDate cycleStart = null, nextStatement = null;
            BigDecimal cycleSpent = null;
            if (card) {
                Integer day = a.getStatementDay();
                if (day != null) {
                    // A statement on day D closes the cycle; the next one starts on D+1
                    LocalDate base = today.getDayOfMonth() > day ? today : today.minusMonths(1);
                    cycleStart = base.withDayOfMonth(day).plusDays(1);
                    nextStatement = cycleStart.plusMonths(1).minusDays(1);
                } else {
                    cycleStart = today.withDayOfMonth(1);
                    nextStatement = today.withDayOfMonth(today.lengthOfMonth());
                }
                cycleSpent = balances.spentOn(a.getAccountId(), cycleStart, today);
            }

            result.add(new AccountBalance(a.getAccountId(), a.getName(), a.getLast4(), a.getAccountType(),
                    a.getOpeningBalance(), a.getOpeningBalanceDate(), balance, source, a.getCreditLimit(),
                    a.getStatementDay(), cycleStart, nextStatement, cycleSpent));
        }
        return result;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public Summary summary(
            @RequestParam("userId") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        BigDecimal spent = BigDecimal.ZERO, income = BigDecimal.ZERO;
        long expenseCount = 0, incomeCount = 0;
        for (var row : queries.totalsByType(userId, from, to)) {
            if ("EXPENSE".equals(row.getType())) { spent = row.getTotal(); expenseCount = row.getCount(); }
            if ("INCOME".equals(row.getType()))  { income = row.getTotal(); incomeCount = row.getCount(); }
        }

        var top = PageRequest.of(0, TOP_N);
        return new Summary(
                from, to, spent, expenseCount, income, incomeCount,
                queries.spendingByCategory(userId, from, to).stream()
                        .map(r -> new CategoryTotal(r.getCategoryId(), r.getName(), r.getIcon(), r.getColor(), r.getTotal(), r.getCount())).toList(),
                queries.spendingByAccount(userId, from, to).stream()
                        .map(r -> new AccountTotal(r.getAccountId(), r.getName(), r.getLast4(), r.getTotal())).toList(),
                queries.spendingByPaymentMethod(userId, from, to).stream()
                        .map(r -> new MethodTotal(r.getPaymentMethod(), r.getTotal())).toList(),
                queries.topMerchants(userId, from, to, top).stream()
                        .map(r -> new MerchantTotal(r.getMerchant().trim(), r.getTotal(), r.getCount())).toList(),
                queries.biggestExpenses(userId, from, to, top).stream()
                        .map(TransactionView::from).toList(),
                queries.spendingByDay(userId, from, to).stream()
                        .map(r -> new DayTotal(r.getDay(), r.getTotal())).toList());
    }

    @GetMapping("/trend")
    @Transactional(readOnly = true)
    public List<MonthTotal> trend(
            @RequestParam("userId") UUID userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // Collect the SQL rows into "2026-10" -> [income, spent]
        Map<YearMonth, BigDecimal[]> byMonth = new HashMap<>();
        for (var row : queries.totalsByMonth(userId, from, to)) {
            var totals = byMonth.computeIfAbsent(YearMonth.of(row.getYear(), row.getMonth()),
                    m -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            if ("TRANSFER".equals(row.getType())) continue; // moving money isn't income or spending
            totals["INCOME".equals(row.getType()) ? 0 : 1] = row.getTotal();
        }

        // Return every month in the range, including empty ones as zero,
        // so the chart has no gaps
        List<MonthTotal> result = new ArrayList<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            var totals = byMonth.getOrDefault(m, new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
            result.add(new MonthTotal(m.toString(), totals[0], totals[1]));
        }
        return result;
    }
}
