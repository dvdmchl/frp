package org.dreamabout.sw.frp.be.module.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Turns the staged records of a connection into balanced accounting transactions: deletes the transactions of records
 * deleted in the source, then resolves the pending records through the mappings and posts the mapped ones.
 *
 * <p>Every record is posted in its own transaction, so a failing record is marked as an error and the run goes on.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportPostingService {

    private final AccImportRecordRepository importRecordRepository;
    private final RecordMappingService recordMappingService;
    private final RecordPostingService recordPostingService;

    public PostingResult post(Long connectionId) {
        var result = PostingResult.EMPTY;
        for (Long importRecordId : importRecordRepository.findDeletedWithTransaction(connectionId)) {
            result = result.plus(attempt(importRecordId, () -> recordPostingService.removeTransaction(importRecordId)));
        }
        for (MappedRecord mapped : recordMappingService.applyMappings(connectionId)) {
            result = result.plus(attempt(mapped.importRecordId(), () -> recordPostingService.post(mapped)));
        }
        log.info("Posted records of connection {}: {}", connectionId, result);
        return result;
    }

    private PostingOutcome attempt(Long importRecordId, Supplier<PostingOutcome> posting) {
        try {
            return posting.get();
        } catch (RuntimeException e) {
            log.warn("Posting of import record {} failed", importRecordId, e);
            recordPostingService.markFailed(importRecordId, messageOf(e));
            return PostingOutcome.FAILED;
        }
    }

    private static String messageOf(RuntimeException e) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
        return Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName());
    }
}
