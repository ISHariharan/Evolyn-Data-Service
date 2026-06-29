package com.evolyn.evolyn_data_service.BankDetails.BankDetailsController;


import com.evolyn.evolyn_data_service.BankDetails.BankDetailsDTO.BankDetailsDTO;
import com.evolyn.evolyn_data_service.BankDetails.BankDetailsEntity;
import com.evolyn.evolyn_data_service.BankDetails.BankDetailsRepository.BankDetailsRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

@RestController
@RequestMapping("/evolyn/api/internal/expenses")
public class BankDetailsController {

    private final BankDetailsRepository bankDetailsRepository;

    public BankDetailsController(BankDetailsRepository bankDetailsRepository){
        this.bankDetailsRepository = bankDetailsRepository;
    }

    @PostMapping("/account/add")
    public ResponseEntity<Object> storeBankDetails(@RequestBody BankDetailsDTO bankDetailsDTO) {
        System.out.print("Request : " + bankDetailsDTO);
        BankDetailsEntity bankDetailsEntity = new BankDetailsEntity();
        bankDetailsEntity.setUserId(bankDetailsDTO.getUserId());
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

        if (bankDetailsRepository.existsByAccountNumberAndUserId(
                bankDetailsEntity.getAccountNumber(),
                bankDetailsEntity.getUserId()
        )) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("Message", "Bank details already exist for this account"));
        }

        try {
            bankDetailsRepository.save(bankDetailsEntity);
        } catch (DataIntegrityViolationException error) {
            System.out.print("Error " + error);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("Message", "Bank details already exist"));
        }

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("Message", "Details Registered Successfully"));
    }

}
