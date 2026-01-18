package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.dreamabout.sw.frp.be.module.accounting.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.TRANSACTIONS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(summary = "Get all transactions", description = "Returns a list of all transactions.")
    @GetMapping
    public ResponseEntity<List<AccTransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @Operation(summary = "Get transaction", description = "Returns a specific transaction by ID.")
    @GetMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccTransactionDto> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @Operation(summary = "Create transaction", description = "Creates a new transaction with journal entries.")
    @PostMapping
    public ResponseEntity<AccTransactionDto> createTransaction(@Valid @RequestBody AccTransactionCreateRequestDto request) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }

    @Operation(summary = "Update transaction", description = "Updates an existing transaction.")
    @PutMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccTransactionDto> updateTransaction(@PathVariable Long id, @Valid @RequestBody AccTransactionCreateRequestDto request) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, request));
    }

    @Operation(summary = "Delete transaction", description = "Deletes a transaction.")
    @DeleteMapping(ApiPath.ID_PARAM)
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok().build();
    }
}
