package org.dreamabout.sw.frp.be.module.accounting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dreamabout.sw.frp.be.domain.ApiPath;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.service.JournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.API_ROOT + ApiPath.ACCOUNTING + ApiPath.JOURNALS)
@RequiredArgsConstructor
@Tag(name = "Accounting", description = "Operations related to accounting module")
public class JournalController {

    private final JournalService journalService;

    @Operation(summary = "Get all journals", description = "Returns a list of all journal entries.")
    @GetMapping
    public ResponseEntity<List<AccJournalDto>> getAllJournals() {
        return ResponseEntity.ok(journalService.getAllJournals());
    }

    @Operation(summary = "Get journal", description = "Returns a specific journal entry by ID.")
    @GetMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccJournalDto> getJournal(@PathVariable Long id) {
        return ResponseEntity.ok(journalService.getJournal(id));
    }

    @Operation(summary = "Update journal", description = "Updates an existing journal entry (date/description only).")
    @PutMapping(ApiPath.ID_PARAM)
    public ResponseEntity<AccJournalDto> updateJournal(@PathVariable Long id, @Valid @RequestBody AccJournalUpdateRequestDto request) {
        return ResponseEntity.ok(journalService.updateJournal(id, request));
    }

    @Operation(summary = "Delete journal", description = "Deletes a journal entry.")
    @DeleteMapping(ApiPath.ID_PARAM)
    public ResponseEntity<Void> deleteJournal(@PathVariable Long id) {
        journalService.deleteJournal(id);
        return ResponseEntity.ok().build();
    }
}
