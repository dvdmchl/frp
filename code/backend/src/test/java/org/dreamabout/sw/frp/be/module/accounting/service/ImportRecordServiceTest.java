package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.ConflictResolution;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccImportRecordDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccMappedItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImportRecordServiceTest extends AbstractImportPostingTest {

    private static final String EDITED = "Edited by hand";

    @Autowired
    private ImportRecordService importRecordService;

    @Test
    void shouldListRecordsOfConnectionInGivenStatusesLatestFirst() {
        var older = stage("r1", CARD, FOOD, "-10");
        var newer = stage("r2", CARD, FOOD, "-20", importRecord -> {
            importRecord.setRecordDate(DATE.plusDays(1));
            importRecord.setStatus(ImportRecordStatus.ERROR);
        });
        stage("r3", CASH, FOOD, "-30", importRecord -> importRecord.setStatus(ImportRecordStatus.POSTED));

        var records = importRecordService.getRecords(connectionId,
                Set.of(ImportRecordStatus.NEW, ImportRecordStatus.ERROR));

        assertThat(records).extracting(AccImportRecordDto::id).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void shouldRejectRecordsOfMissingConnection() {
        var statuses = Set.of(ImportRecordStatus.NEW);

        assertThatThrownBy(() -> importRecordService.getRecords(-1L, statuses))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Connection not found");
    }

    @Test
    void shouldDescribeRecordWithItsMappingsAndRawPayload() {
        var expense = stage("r1", CASH, FOOD, "-100", importRecord -> {
            importRecord.setBaseAmount(new BigDecimal("-4.00"));
            importRecord.setBaseCurrencyCode("EUR");
            importRecord.setRawPayload("{\"note\": \"Lunch\"}");
        });

        var detail = importRecordService.getRecordDetail(connectionId, expense.getId());

        assertThat(detail.importRecord().externalId()).isEqualTo("r1");
        assertThat(detail.account()).isEqualTo(new AccMappedItemDto(CASH, "Cash", "Wallet cash"));
        assertThat(detail.category()).isEqualTo(new AccMappedItemDto(FOOD, "Food", "Groceries"));
        assertThat(detail.transferAccount()).isNull();
        assertThat(detail.baseAmount()).isEqualByComparingTo("-4");
        assertThat(detail.baseCurrencyCode()).isEqualTo("EUR");
        assertThat(detail.firstSeenAt()).isNotNull();
        assertThat(detail.rawPayload()).contains("Lunch");
    }

    @Test
    void shouldDescribeTransferByAccountOfOtherLegAndShowUnmappedAccount() {
        var outgoing = stage("t-out", CARD, null, "-500", transferLeg());
        stage("t-in", BANK, null, "500", transferLeg());

        var detail = importRecordService.getRecordDetail(connectionId, outgoing.getId());

        assertThat(detail.account()).isEqualTo(new AccMappedItemDto(CARD, "Card", null));
        assertThat(detail.category()).isNull();
        assertThat(detail.transferAccount()).isEqualTo(new AccMappedItemDto(BANK, "Bank", "Bank account"));
    }

    @Test
    void shouldNotDescribeTransferByOtherLegDeletedInSource() {
        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        stage("t-in", BANK, null, "500",
                transferLeg().andThen(importRecord -> importRecord.setStatus(ImportRecordStatus.DELETED)));

        var detail = importRecordService.getRecordDetail(connectionId, outgoing.getId());

        assertThat(detail.transferAccount()).isNull();
    }

    @Test
    void shouldRejectDetailOfRecordOfOtherConnection() {
        var recordId = stage("r1", CASH, FOOD, "-100").getId();
        var otherConnectionId = connectionId + 1;

        assertThatThrownBy(() -> importRecordService.getRecordDetail(otherConnectionId, recordId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Import record not found: " + recordId);
    }

    @Test
    void shouldPostFailedRecordWhenRetried() {
        var expense = stage("r1", CARD, FOOD, "-100", importRecord -> {
            importRecord.setCurrencyCode("EUR");
            importRecord.setBaseAmount(new BigDecimal("-2500"));
            importRecord.setBaseCurrencyCode("CZK");
        });
        postingService.post(connectionId);
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.ERROR);
        currencyService.createCurrency(new AccCurrencyCreateRequestDto("EUR", "Euro", false, 2));
        map(CARD, createAccount("Card", AccAcountType.ASSET, "EUR"));

        var retried = importRecordService.retry(connectionId, expense.getId());

        assertThat(retried.status()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(retried.errorMessage()).isNull();
        assertThat(retried.transactionId()).isNotNull();
    }

    @Test
    void shouldRejectRetryOfRecordThatDidNotFail() {
        var recordId = stage("r1", CARD, FOOD, "-100").getId();

        assertThatThrownBy(() -> importRecordService.retry(connectionId, recordId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Import record " + recordId + " is not in status ERROR in connection " + connectionId);
    }

    @Test
    void shouldSkipIgnoredRecordWhenPosting() {
        var unmapped = stage("r1", CARD, FOOD, "-100");

        var ignored = importRecordService.ignore(connectionId, unmapped.getId());
        map(CARD, createAccount("Card", AccAcountType.ASSET, "CZK"));
        postingService.post(connectionId);

        assertThat(ignored.status()).isEqualTo(ImportRecordStatus.SKIPPED);
        assertThat(reload(unmapped).getStatus()).isEqualTo(ImportRecordStatus.SKIPPED);
        assertThat(transactionService.getAllTransactions()).isEmpty();
    }

    @Test
    void shouldRejectIgnoreOfPostedRecord() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        var recordId = expense.getId();

        assertThatThrownBy(() -> importRecordService.ignore(connectionId, recordId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Import record " + recordId + " is not waiting to be posted in connection " + connectionId);
    }

    @Test
    void shouldRejectIgnoreOfRecordOfOtherConnection() {
        var recordId = stage("r1", CARD, FOOD, "-100").getId();
        var otherConnectionId = connectionId + 1;

        assertThatThrownBy(() -> importRecordService.ignore(otherConnectionId, recordId))
                .isInstanceOf(IllegalStateException.class);
        assertThat(importRecordRepository.findById(recordId).orElseThrow().getStatus())
                .isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldKeepTransactionChangedInFrpAsPostedState() {
        var expense = conflictOverChangedTransaction();
        var transactionId = reload(expense).getTransactionId();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(), ConflictResolution.KEEP_FRP);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(resolved.errorMessage()).isNull();
        var transaction = transactionService.getTransaction(transactionId);
        assertThat(transaction.description()).isEqualTo(EDITED);
        assertThat(journalsOf(transaction)).containsExactlyInAnyOrder(credit(cashId, "100"), debit(groceriesId, "100"));
    }

    @Test
    void shouldPostLaterSourceChangeOverKeptTransaction() {
        var expense = conflictOverChangedTransaction();
        importRecordService.resolveConflict(connectionId, expense.getId(), ConflictResolution.KEEP_FRP);

        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-300")));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(1, 0, 0, 0));
        assertThat(journalsOf(transactionOf(expense)))
                .containsExactlyInAnyOrder(credit(cashId, "300"), debit(groceriesId, "300"));
    }

    @Test
    void shouldOverwriteTransactionChangedInFrpWithSource() {
        var expense = conflictOverChangedTransaction();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(),
                ConflictResolution.USE_SOURCE);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(journalsOf(transactionOf(expense)))
                .containsExactlyInAnyOrder(credit(cashId, "250"), debit(groceriesId, "250"));
    }

    @Test
    void shouldRecreateTransactionDeletedInFrpFromSource() {
        var expense = conflictOverDeletedTransaction();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(),
                ConflictResolution.USE_SOURCE);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(journalsOf(transactionOf(expense)))
                .containsExactlyInAnyOrder(credit(cashId, "250"), debit(groceriesId, "250"));
    }

    @Test
    void shouldSkipRecordWhenDeletionInFrpIsKept() {
        var expense = conflictOverDeletedTransaction();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(), ConflictResolution.KEEP_FRP);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.SKIPPED);
        assertThat(transactionService.getAllTransactions()).isEmpty();
    }

    @Test
    void shouldDeleteTransactionChangedInFrpWhenSourceDeletionIsUsed() {
        var expense = conflictOverDeletionInSource();
        var transactionId = reload(expense).getTransactionId();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(),
                ConflictResolution.USE_SOURCE);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.DELETED);
        assertThat(resolved.transactionId()).isNull();
        assertThatThrownBy(() -> transactionService.getTransaction(transactionId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldLetGoOfTransactionWhenItIsKeptDespiteSourceDeletion() {
        var expense = conflictOverDeletionInSource();
        var transactionId = reload(expense).getTransactionId();

        var resolved = importRecordService.resolveConflict(connectionId, expense.getId(), ConflictResolution.KEEP_FRP);

        assertThat(resolved.status()).isEqualTo(ImportRecordStatus.DELETED);
        assertThat(resolved.transactionId()).isNull();
        assertThat(transactionService.getTransaction(transactionId).description()).isEqualTo(EDITED);
        assertThat(postingService.post(connectionId)).isEqualTo(PostingResult.EMPTY);
    }

    @Test
    void shouldResolveBothLegsOfTransferInConflict() {
        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        var incoming = stage("t-in", BANK, null, "500", transferLeg());
        postingService.post(connectionId);
        editInFrp(reload(outgoing).getTransactionId(), EDITED);
        changeInSource(outgoing, importRecord -> importRecord.setNote("Savings"));
        changeInSource(incoming, importRecord -> importRecord.setNote("Savings"));
        postingService.post(connectionId);
        assertThat(reload(incoming).getStatus()).isEqualTo(ImportRecordStatus.CONFLICT);

        importRecordService.resolveConflict(connectionId, outgoing.getId(), ConflictResolution.USE_SOURCE);

        assertThat(reload(outgoing).getStatus()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(reload(incoming).getStatus()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(transactionOf(outgoing).description()).isEqualTo("Savings");
    }

    @Test
    void shouldRejectResolutionOfRecordNotInConflict() {
        var recordId = stage("r1", CASH, FOOD, "-100").getId();

        assertThatThrownBy(() -> importRecordService.resolveConflict(connectionId, recordId,
                ConflictResolution.USE_SOURCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Import record " + recordId + " is not in status CONFLICT in connection " + connectionId);
    }

    /**
     * Expense of 100 posted, its transaction edited in FRP, then changed to 250 in the source.
     */
    private AccImportRecordEntity conflictOverChangedTransaction() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        editInFrp(reload(expense).getTransactionId(), EDITED);
        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-250")));
        postingService.post(connectionId);
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.CONFLICT);
        return expense;
    }

    /**
     * Expense of 100 posted, its transaction deleted in FRP, then changed to 250 in the source.
     */
    private AccImportRecordEntity conflictOverDeletedTransaction() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        transactionService.deleteTransaction(reload(expense).getTransactionId());
        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-250")));
        postingService.post(connectionId);
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.CONFLICT);
        return expense;
    }

    /**
     * Expense posted, its transaction edited in FRP, then deleted in the source.
     */
    private AccImportRecordEntity conflictOverDeletionInSource() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        editInFrp(reload(expense).getTransactionId(), EDITED);
        changeInSource(expense, importRecord -> {
            importRecord.setStatus(ImportRecordStatus.DELETED);
            importRecord.setSourceState(ExternalRecordState.DELETED);
        });
        postingService.post(connectionId);
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.CONFLICT);
        return expense;
    }
}
