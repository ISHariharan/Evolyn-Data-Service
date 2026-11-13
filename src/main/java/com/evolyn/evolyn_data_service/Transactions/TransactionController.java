package com.evolyn.evolyn_data_service.Transactions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.evolyn.evolyn_data_service.Accounts.AccountEntity;
import com.evolyn.evolyn_data_service.Accounts.AccountRepository;
import com.evolyn.evolyn_data_service.Categories.CategoryEntity;
import com.evolyn.evolyn_data_service.Categories.CategoryRepository;

/**
 * Internal endpoints: only Evolyn-API calls these, never the browser.
 *   POST   /transactions                     create
 *   GET    /transactions?userId=&filters     list one page, newest first
 *   GET    /transactions/{id}?userId=        one transaction
 *   PUT    /transactions/{id}                update
 *   DELETE /transactions/{id}?userId=        delete
 */
@RestController
@RequestMapping(value = "/evolyn/api/internal/transactions", produces = "application/json")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    public TransactionController(TransactionRepository transactionRepository, AccountRepository accountRepository,
                                 CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> createTransaction(@RequestBody TransactionRequest request) {
        Optional<AccountEntity> account = accountRepository.findByAccountIdAndUserId(request.accountId(), request.userId());
        if (account.isEmpty()) return accountNotFound();
        Object links = links(request, account.get());
        if (links instanceof ResponseEntity<?> error) return castError(error);

        TransactionEntity entity = new TransactionEntity();
        entity.setTransactionId(UUID.randomUUID());
        entity.setUserId(request.userId());
        entity.setSource("MANUAL");
        entity.setCreatedAt(Instant.now());
        apply(entity, request, account.get(), (Links) links);
        entity.setUpdatedAt(entity.getCreatedAt()); // brand new: "never edited"

        TransactionEntity saved = transactionRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(TransactionView.from(saved));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<TransactionView> listTransactions(
            @RequestParam("userId") UUID userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Newest first; createdAt breaks ties between rows on the same day
        Sort newestFirst = Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"));
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), newestFirst);

        var result = transactionRepository
                .findAll(TransactionSpecs.matching(userId, type, categoryId, accountId, from, to), pageable)
                .map(TransactionView::from);
        return PageResponse.of(result);
    }

    @GetMapping("/{transactionId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getTransaction(@PathVariable UUID transactionId, @RequestParam("userId") UUID userId) {
        return transactionRepository.findByTransactionIdAndUserId(transactionId, userId)
                .<ResponseEntity<Object>>map(t -> ResponseEntity.ok(TransactionView.from(t)))
                .orElseGet(TransactionController::transactionNotFound);
    }

    @PutMapping("/{transactionId}")
    @Transactional
    public ResponseEntity<Object> updateTransaction(@PathVariable UUID transactionId, @RequestBody TransactionRequest request) {
        Optional<TransactionEntity> existing = transactionRepository.findByTransactionIdAndUserId(transactionId, request.userId());
        if (existing.isEmpty()) return transactionNotFound();

        Optional<AccountEntity> account = accountRepository.findByAccountIdAndUserId(request.accountId(), request.userId());
        if (account.isEmpty()) return accountNotFound();
        Object links = links(request, account.get());
        if (links instanceof ResponseEntity<?> error) return castError(error);

        TransactionEntity entity = existing.get();
        apply(entity, request, account.get(), (Links) links);
        return ResponseEntity.ok(TransactionView.from(transactionRepository.save(entity)));
    }

    @DeleteMapping("/{transactionId}")
    @Transactional
    public ResponseEntity<Object> deleteTransaction(@PathVariable UUID transactionId, @RequestParam("userId") UUID userId) {
        Optional<TransactionEntity> existing = transactionRepository.findByTransactionIdAndUserId(transactionId, userId);
        if (existing.isEmpty()) return transactionNotFound();
        transactionRepository.delete(existing.get());
        return ResponseEntity.noContent().build();
    }

    // Copies the editable fields from the request onto the entity
    private static void apply(TransactionEntity entity, TransactionRequest request, AccountEntity account,
                              Links links) {
        entity.setAccount(account);
        entity.setToAccount(links.toAccount());
        entity.setCategory(links.category()); // also copies its name into the backup column
        if (links.category() == null) entity.setCategoryName("Transfer"); // the backup column can't be empty
        entity.setType(request.type());
        entity.setAmount(request.amount());
        entity.setTransactionDate(request.transactionDate());
        entity.setMerchant(request.merchant().trim());
        String notes = request.notes();
        entity.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        entity.setPaymentMethod(request.paymentMethod());
        entity.setUpdatedAt(Instant.now());
    }

    // What a transaction points at besides its account
    private record Links(CategoryEntity category, AccountEntity toAccount) {}

    /**
     * EXPENSE / INCOME: needs one of the user's categories of the same type.
     * TRANSFER: needs a second account (where the money goes) and no category,
     * e.g. paying a credit card bill, withdrawing cash, topping up a wallet.
     * Returns Links, or an error response.
     */
    private Object links(TransactionRequest request, AccountEntity from) {
        if ("TRANSFER".equals(request.type())) {
            if (request.toAccountId() == null) return badRequest("Choose the account the money goes to");
            Optional<AccountEntity> to = accountRepository.findByAccountIdAndUserId(request.toAccountId(), request.userId());
            if (to.isEmpty()) return accountNotFound();
            if (to.get().getAccountId().equals(from.getAccountId())) return badRequest("Choose two different accounts");
            return new Links(null, to.get());
        }
        Optional<CategoryEntity> category = categoryFor(request);
        if (category.isEmpty()) return categoryInvalid();
        return new Links(category.get(), null);
    }

    @SuppressWarnings("unchecked")
    private static ResponseEntity<Object> castError(ResponseEntity<?> error) {
        return (ResponseEntity<Object>) error;
    }

    private static ResponseEntity<Object> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    // The category must belong to this user and match the transaction's type
    // (no "Salary" on an expense)
    private Optional<CategoryEntity> categoryFor(TransactionRequest request) {
        if (request.categoryId() == null) return Optional.empty();
        return categoryRepository.findByCategoryIdAndUserId(request.categoryId(), request.userId())
                .filter(c -> c.getType().equals(request.type()));
    }

    private static ResponseEntity<Object> categoryInvalid() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Choose one of your categories that matches the transaction type"));
    }

    private static ResponseEntity<Object> transactionNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Transaction not found"));
    }

    private static ResponseEntity<Object> accountNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Account not found"));
    }
}
