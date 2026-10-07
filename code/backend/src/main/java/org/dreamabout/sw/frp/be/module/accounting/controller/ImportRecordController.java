package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConflictResolutionRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDetailDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDto;
import org.dreamabout.sw.frp.be.module.accounting.service.ImportRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.CONNECTIONS + ApiPath.ID_PARAM + ApiPath.RECORDS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class ImportRecordController {

    private final ImportRecordService importRecordService;

    @Operation(summary = "Get import records",
            description = "Returns the records staged from a connection in the given statuses, the latest first. "
                    + "Records waiting for a mapping are NEW.")
    @GetMapping
    public ResponseEntity<List<AccImportRecordDto>> getRecords(@PathVariable Long id,
                                                               @RequestParam Set<ImportRecordStatus> status) {
        return ResponseEntity.ok(importRecordService.getRecords(id, status));
    }

    @Operation(summary = "Get import record detail",
            description = "Returns a staged record with the mappings of its account and category, the account of the "
                    + "other leg of a transfer and the representation of the record in the source.")
    @GetMapping(ApiPath.RECORD_ID_PARAM)
    public ResponseEntity<AccImportRecordDetailDto> getRecordDetail(@PathVariable Long id,
                                                                    @PathVariable Long recordId) {
        return ResponseEntity.ok(importRecordService.getRecordDetail(id, recordId));
    }

    @Operation(summary = "Retry import record",
            description = "Posts a failed record again together with the other pending records of the connection.")
    @PostMapping(ApiPath.RECORD_ID_PARAM + ApiPath.RETRY)
    public ResponseEntity<AccImportRecordDto> retryRecord(@PathVariable Long id, @PathVariable Long recordId) {
        return ResponseEntity.ok(importRecordService.retry(id, recordId));
    }

    @Operation(summary = "Ignore import record",
            description = "Skips a record waiting to be posted until it changes in the source.")
    @PostMapping(ApiPath.RECORD_ID_PARAM + ApiPath.IGNORE)
    public ResponseEntity<AccImportRecordDto> ignoreRecord(@PathVariable Long id, @PathVariable Long recordId) {
        return ResponseEntity.ok(importRecordService.ignore(id, recordId));
    }

    @Operation(summary = "Resolve import record conflict",
            description = "Keeps the transaction as changed in FRP, or applies the record from the source.")
    @PostMapping(ApiPath.RECORD_ID_PARAM + ApiPath.RESOLVE)
    public ResponseEntity<AccImportRecordDto> resolveConflict(
            @PathVariable Long id, @PathVariable Long recordId,
            @Valid @RequestBody AccConflictResolutionRequestDto request) {
        return ResponseEntity.ok(importRecordService.resolveConflict(id, recordId, request.resolution()));
    }
}
