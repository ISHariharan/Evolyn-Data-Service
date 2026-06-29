package com.evolyn.evolyn_data_service.BankDetails.BankDetailsRepository;

import com.evolyn.evolyn_data_service.BankDetails.BankDetailsEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankDetailsRepository extends JpaRepository<BankDetailsEntity, String> {
    boolean existsByAccountNumberAndUserId(String accountNumber, String userId);
}
