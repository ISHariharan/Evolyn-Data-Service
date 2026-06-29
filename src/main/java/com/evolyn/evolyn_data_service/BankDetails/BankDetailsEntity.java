package com.evolyn.evolyn_data_service.BankDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name="user_bank_details",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "user_id"),
                @UniqueConstraint(columnNames = "user_account_name"),
                @UniqueConstraint(columnNames = "user_account_linked_credit_card_number"),
                @UniqueConstraint(columnNames = "user_account_linked_debit_card_number"),
        }
)
public class BankDetailsEntity {
    @Id
    @Column(name = "user_id", updatable = false, nullable = false, unique = true)
    public String userId;

    @Column(name = "user_account_name")
    public String accountName;

    @Column(name = "user_account_number", unique = true)
    public String accountNumber;

    @Column(name = "user_bank_name")
    public String bankName;

    @Column(name = "user_account_type")
    public String accountType;

    @Column(name = "user_account_IFSC_Code")
    public String accountIFSCCode;

    @Column(name = "user_account_balance")
    public String accountBalance;

    @Column(name = "user_account_linked_credit_card_number", unique = true)
    public String linkedCreditCardNumber;

    @Column(name = "user_account_linked_credit_card_limit")
    public String linkedCreditCardLimit;

    @Column(name = "user_account_linked_debit_card_number", unique = true)
    public String linkedDebitCardNumber;

    @Column(name = "user_notes")
    public String notes;

    @Column(name = "user_cash_details")
    public String cashDetails;

    public String getUserId() {
        return this.userId;
    }

    public String getUserID() {
        return this.userId;
    }

    public String getAccountName() {
        return this.accountName;
    }

    public String getAccountNumber() {
        return this.accountNumber;
    }

    public String getBankName() {
        return this.bankName;
    }

    public String getAccountType() {
        return this.accountType;
    }

    public String getAccountIFSCCode() {
        return this.accountIFSCCode;
    }

    public String getAccountBalance() {
        return this.accountBalance;
    }

    public String getLinkedCreditCardNumber() {
        return this.linkedCreditCardNumber;
    }

    public String getLinkedCreditCardLimit() {
        return this.linkedCreditCardLimit;
    }

    public String getLinkedDebitCardNumber() {
        return this.linkedDebitCardNumber;
    }

    public String getNotes() {
        return this.notes;
    }

    public String getCashDetails() {
        return this.cashDetails;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setAccountIFSCCode(String accountIFSCCode) {
        this.accountIFSCCode = accountIFSCCode;
    }

    public void setAccountBalance(String accountBalance) {
        this.accountBalance = accountBalance;
    }

    public void setLinkedCreditCardNumber(String linkedCreditCardNumber) {
        this.linkedCreditCardNumber = linkedCreditCardNumber;
    }

    public void setLinkedCreditCardLimit(String linkedCreditCardLimit) {
        this.linkedCreditCardLimit = linkedCreditCardLimit;
    }

    public void setLinkedDebitCardNumber(String linkedDebitCardNumber) {
        this.linkedDebitCardNumber = linkedDebitCardNumber;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCashDetails(String cashDetails) {
        this.cashDetails = cashDetails;
    }
}
