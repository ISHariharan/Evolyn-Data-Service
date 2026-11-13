package com.evolyn.evolyn_data_service.Transactions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.evolyn.evolyn_data_service.Accounts.AccountEntity;
import com.evolyn.evolyn_data_service.Categories.CategoryEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One row = one money movement (an expense or an income).
 * Many transactions belong to one account (many-to-one), stored as the
 * account_id foreign key column.
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transactions_user_date", columnList = "user_id, transaction_date"),
        @Index(name = "idx_transactions_account", columnList = "account_id"),
        @Index(name = "idx_transactions_category", columnList = "category_id"),
        @Index(name = "idx_transactions_to_account", columnList = "to_account_id")
    }
)
public class TransactionEntity {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // LAZY: only load the account row when we actually read it
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    // TRANSFER only: the account the money moves INTO (e.g. paying a card bill from
    // a bank account: account = bank, toAccount = card). Null for expenses and income.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private AccountEntity toAccount;

    // "EXPENSE", "INCOME" or "TRANSFER"
    @Column(name = "type", nullable = false, length = 10)
    private String type;

    // NUMERIC(12,2): exact money arithmetic, never float/double
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    // The category this transaction belongs to (source of truth).
    // Nullable in the database only because the column was added to a table that
    // already had rows; CategoryBackfill fills it for old rows, and the app always
    // sets it for new ones.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    // Copy of the category's name, kept in sync. This was the original column before
    // categories became editable; we keep it as a readable backup (and the live
    // database has it as NOT NULL).
    @Column(name = "category", nullable = false, length = 50)
    private String categoryName;

    // Who you paid / who paid you, e.g. "Swiggy", "Acme Corp"
    @Column(name = "merchant", nullable = false, length = 100)
    private String merchant;

    @Column(name = "notes", length = 500)
    private String notes;

    // UPI, CREDIT_CARD, DEBIT_CARD, NET_BANKING, CASH
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    // MANUAL for now; SMS / STATEMENT later
    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public AccountEntity getToAccount() { return toAccount; }
    public void setToAccount(AccountEntity toAccount) { this.toAccount = toAccount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public CategoryEntity getCategory() { return category; }
    public void setCategory(CategoryEntity category) {
        this.category = category;
        this.categoryName = category == null ? this.categoryName : category.getName();
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
