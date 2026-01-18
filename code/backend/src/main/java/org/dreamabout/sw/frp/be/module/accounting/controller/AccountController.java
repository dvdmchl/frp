package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccNodeMoveRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Create account", description = "Creates a new account or placeholder node.")
    @PostMapping(ApiPath.ACCOUNTS)
    public ResponseEntity<AccNodeDto> createAccount(@Valid @RequestBody AccAccountCreateRequestDto request) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    @Operation(summary = "Get account tree", description = "Returns the entire account tree.")
    @GetMapping(ApiPath.ACCOUNTS + ApiPath.ACCOUNTS_TREE)
    public ResponseEntity<List<AccNodeDto>> getTree() {
        return ResponseEntity.ok(accountService.getTree());
    }

    @Operation(summary = "Update account", description = "Updates an existing account.")
    @PutMapping(ApiPath.ACCOUNTS + ApiPath.ID_PARAM)
    public ResponseEntity<AccNodeDto> updateAccount(@PathVariable Long id, @Valid @RequestBody AccAccountCreateRequestDto request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @Operation(summary = "Move account", description = "Moves an account to a new parent and/or reorders it.")
    @PostMapping(ApiPath.ACCOUNTS + ApiPath.ID_PARAM + ApiPath.MOVE)
    public ResponseEntity<Void> moveAccount(@PathVariable Long id, @Valid @RequestBody AccNodeMoveRequestDto request) {
        accountService.moveNode(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete account", description = "Deletes an account node.")
    @DeleteMapping(ApiPath.ACCOUNTS + ApiPath.ID_PARAM)
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteNode(id);
        return ResponseEntity.ok().build();
    }
}
