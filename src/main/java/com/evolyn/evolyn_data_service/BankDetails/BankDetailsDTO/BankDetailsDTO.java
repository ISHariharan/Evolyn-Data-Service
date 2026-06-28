package com.evolyn.evolyn_data_service.BankDetails.BankDetailsDTO;

public class BankDetailsDTO {
    public String userId;
    public accountDetailsDTO accountDetails;
    public cashDetailsDTO cashDetails;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public accountDetailsDTO getAccountDetails() {
        return accountDetails;
    }

    public void setAccountDetails(accountDetailsDTO accountDetails) {
        this.accountDetails = accountDetails;
    }

    public cashDetailsDTO getCashDetails() {
        return cashDetails;
    }

    public void setCashDetails(cashDetailsDTO cashDetails) {
        this.cashDetails = cashDetails;
    }
}
