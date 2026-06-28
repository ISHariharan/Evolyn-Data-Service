package com.evolyn.evolyn_data_service.BankDetails.BankDetailsController;


import com.evolyn.evolyn_data_service.BankDetails.BankDetailsDTO.BankDetailsDTO;
import com.evolyn.evolyn_data_service.BankDetails.BankDetailsEntity;
import com.evolyn.evolyn_data_service.BankDetails.BankDetailsRepository.BankDetailsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

@RestController
@RequestMapping("/evolyn/api/internal/expenses")
public class BankDetailsController {

    public BankDetailsRepository bankDetailsRepository;
    public BankDetailsController(BankDetailsRepository bankDetailsRepository){}
    @PostMapping("/account/add")
    public ResponseEntity<Object> storeBankDetails(BankDetailsDTO bankDetailsDTO) {
        BankDetailsEntity bankDetailsEntity = new BankDetailsEntity();
        bankDetailsEntity.setUser_id(bankDetailsDTO.getUserId());
        bankDetailsEntity.setCashDetails(bankDetailsDTO.getCashDetails().getBalanceAmount());
        bankDetailsEntity.setAccountNumber(bankDetailsDTO.getAccountDetails().getAccountNumber());
        bankDetailsEntity.setAccountType(bankDetailsDTO.getAccountDetails().getAccountType());
        bankDetailsEntity.setAccountBalance(bankDetailsDTO.getAccountDetails().getAccountBalance());
        bankDetailsEntity.setBankName(bankDetailsDTO.getAccountDetails().getBankName());
        bankDetailsEntity.setAccountName(bankDetailsDTO.getAccountDetails().getAccountName());
        bankDetailsEntity.setAccountIFSCCode(bankDetailsDTO.getAccountDetails().getAccountIFSCCode());
        bankDetailsEntity.setLinkedCreditCardNumber(bankDetailsDTO.getAccountDetails().getLinkedCreditCardNumber());
        bankDetailsEntity.setLinkedCreditCardLimit(bankDetailsDTO.getAccountDetails().getLinkedCreditCardLimit());
        bankDetailsEntity.setLinkedDebitCardNumber(bankDetailsDTO.getAccountDetails().getLinkedDebitCardNumber());
        bankDetailsEntity.setNotes(bankDetailsDTO.getAccountDetails().getNotes());

        if (bankDetailsRepository.existsByAccountNumberAndUserID(bankDetailsEntity.getAccountNumber(), bankDetailsEntity.getUserID())) {
            ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("Message", "Details Registered Successfully"));
    }

}
