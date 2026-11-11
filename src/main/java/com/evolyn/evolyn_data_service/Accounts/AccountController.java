package com.evolyn.evolyn_data_service.Accounts;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Transactions.TransactionRepository;

/** Internal endpoints for a user's accounts (only Evolyn-API calls these). */
@RestController
@RequestMapping(value = "/evolyn/api/internal/accounts", produces = "application/json")
public class AccountController {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountController(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<AccountEntity> listAccounts(@RequestParam("userId") UUID userId) {
        return accountRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @PostMapping
    public ResponseEntity<AccountEntity> createAccount(@RequestBody AccountRequest request) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(UUID.randomUUID());
        account.setUserId(request.userId());
        account.setCreatedAt(Instant.now());
        apply(account, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountRepository.save(account));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<Object> updateAccount(@PathVariable UUID accountId, @RequestBody AccountRequest request) {
        return accountRepository.findByAccountIdAndUserId(accountId, request.userId())
                .<ResponseEntity<Object>>map(account -> {
                    apply(account, request);
                    return ResponseEntity.ok(accountRepository.save(account));
                })
                .orElseGet(() -> notFound());
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Object> deleteAccount(@PathVariable UUID accountId, @RequestParam("userId") UUID userId) {
        var account = accountRepository.findByAccountIdAndUserId(accountId, userId);
        if (account.isEmpty()) return notFound();

        // Deleting an account would orphan its transactions, so refuse instead
        long used = transactionRepository.countByAccount_AccountId(accountId);
        if (used > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "This account has " + used + " transaction(s). Move or delete them first."));
        }
        accountRepository.delete(account.get());
        return ResponseEntity.noContent().build();
    }

    private static void apply(AccountEntity account, AccountRequest request) {
        account.setName(request.name().trim());
        account.setAccountType(request.accountType());
        String last4 = request.last4();
        account.setLast4(last4 == null || last4.isBlank() ? null : last4.trim());
    }

    private static ResponseEntity<Object> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Account not found"));
    }
}
