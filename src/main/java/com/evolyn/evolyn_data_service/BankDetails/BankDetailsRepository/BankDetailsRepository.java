package com.evolyn.evolyn_data_service.BankDetails.BankDetailsRepository;
package com.evolyn.evolyn_data_service.BankDetails.BankDetailsEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface BankDetailsRepository extends JpaRepository<BankDetailsEntity, UUID> {
    public boolean existsByAccountNumberAndUserID(String AccountNumber, String UserID);
}
