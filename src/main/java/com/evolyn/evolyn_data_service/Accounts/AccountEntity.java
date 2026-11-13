package com.evolyn.evolyn_data_service.Accounts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A place money moves in or out of: a bank account, a credit card, cash, a wallet.
 * One user can have any number of accounts (one-to-many: user -> accounts).
 * We only ever store the LAST 4 digits, never a full account or card number.
 */
@Entity
@Table(
    name = "accounts",
    indexes = @Index(name = "idx_accounts_user", columnList = "user_id")
)
public class AccountEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // e.g. "HDFC Bank", "ICICI Amazon Pay Card", "Cash"
    @Column(name = "name", nullable = false, length = 60)
    private String name;

    // BANK, CREDIT_CARD, CASH, WALLET
    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    // e.g. "1234" (null for cash)
    @Column(name = "last4", length = 4)
    private String last4;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // ── Balance tracking (all optional) ──────────────────────────────────────
    // The balance at the START of openingBalanceDate. Every transaction dated on or
    // after that day moves it. For a credit card it's the amount you OWE.
    @Column(name = "opening_balance", precision = 12, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_balance_date")
    private LocalDate openingBalanceDate;

    // Credit cards only
    @Column(name = "credit_limit", precision = 12, scale = 2)
    private BigDecimal creditLimit;

    // Credit cards only: day of the month the statement is generated (1-28)
    @Column(name = "statement_day")
    private Integer statementDay;

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getLast4() { return last4; }
    public void setLast4(String last4) { this.last4 = last4; }

    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }

    public LocalDate getOpeningBalanceDate() { return openingBalanceDate; }
    public void setOpeningBalanceDate(LocalDate openingBalanceDate) { this.openingBalanceDate = openingBalanceDate; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public Integer getStatementDay() { return statementDay; }
    public void setStatementDay(Integer statementDay) { this.statementDay = statementDay; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
