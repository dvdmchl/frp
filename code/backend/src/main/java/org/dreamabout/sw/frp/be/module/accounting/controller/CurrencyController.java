package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.service.CurrencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CURRENCIES)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class CurrencyController {

    private final CurrencyService currencyService;

    @Operation(summary = "Get all currencies", description = "Returns a list of all available currencies.")
    @GetMapping
    public ResponseEntity<List<AccCurrencyDto>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @Operation(summary = "Create currency", description = "Creates a new currency.")
    @PostMapping
    public ResponseEntity<AccCurrencyDto> createCurrency(@Valid @RequestBody AccCurrencyCreateRequestDto request) {
        return ResponseEntity.ok(currencyService.createCurrency(request));
    }

    @Operation(summary = "Update currency", description = "Updates an existing currency.")
    @PutMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccCurrencyDto> updateCurrency(@PathVariable Long id, @Valid @RequestBody AccCurrencyUpdateRequestDto request) {
        return ResponseEntity.ok(currencyService.updateCurrency(id, request));
    }

    @Operation(summary = "Delete currency", description = "Deletes a currency if not in use.")
    @DeleteMapping(ApiPath.ID_PARAM)
    public ResponseEntity<Void> deleteCurrency(@PathVariable Long id) {
        currencyService.deleteCurrency(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Set base currency", description = "Sets a specific currency as the base currency.")
    @PutMapping(ApiPath.ID_PARAM + ApiPath.ACTIVE)
    public ResponseEntity<Void> setBaseCurrency(@PathVariable Long id) {
        currencyService.setBaseCurrency(id);
        return ResponseEntity.ok().build();
    }
}
