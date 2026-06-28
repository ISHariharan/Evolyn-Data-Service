package com.evolyn.evolyn_data_service.BankDetails.BankDetailsDTO;

public class accountDetailsDTO {
    public String accountNumber;
    public String accountType;
    public String accountName;
    public String bankName;
    public String accountIFSCCode;
    public String accountBalance;
    public String linkedCreditCardNumber;
    public String linkedCreditCardLimit;
    public String linkedDebitCardNumber;
    public String Notes;

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
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
        this.Notes = notes;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }
    public String getAccountName() {
        return accountName;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountIFSCCode() {
        return accountIFSCCode;
    }

    public String getAccountBalance() {
        return accountBalance;
    }

    public String getLinkedCreditCardNumber() {
        return linkedCreditCardNumber;
    }

    public String getLinkedCreditCardLimit() {
        return linkedCreditCardLimit;
    }

    public String getLinkedDebitCardNumber() {
        return linkedDebitCardNumber;
    }

    public String getNotes() {
        return Notes;
    }
}
