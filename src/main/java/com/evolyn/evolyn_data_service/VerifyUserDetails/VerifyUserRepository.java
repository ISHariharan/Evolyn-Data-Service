package com.evolyn.evolyn_data_service.VerifyUserDetails;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.evolyn.evolyn_data_service.AuthStoreService.AuthStoreDAO;


@Repository
public interface VerifyUserRepository extends JpaRepository<AuthStoreDAO, UUID> {
    boolean existsByEmail(String email);
    Optional<AuthStoreDAO> findByEmail(String email);
}
