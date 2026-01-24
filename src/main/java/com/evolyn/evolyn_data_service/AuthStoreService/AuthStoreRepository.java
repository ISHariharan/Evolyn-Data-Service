package com.evolyn.evolyn_data_service.AuthStoreService;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthStoreRepository extends JpaRepository<AuthStoreDTO, UUID> {
    boolean existsByEmail(String email);
}
