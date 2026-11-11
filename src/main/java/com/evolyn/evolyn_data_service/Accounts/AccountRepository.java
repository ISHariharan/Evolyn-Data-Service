package com.evolyn.evolyn_data_service.Accounts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    List<AccountEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    // Always look rows up by id AND owner, so one user can never touch another user's account
    Optional<AccountEntity> findByAccountIdAndUserId(UUID accountId, UUID userId);
}
