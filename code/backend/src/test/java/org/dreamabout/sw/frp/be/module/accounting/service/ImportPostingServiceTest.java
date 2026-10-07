package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ImportPostingServiceTest extends AbstractImportPostingTest {

    @Test
    void shouldPostExpenseAsCreditOfAccountAndDebitOfCategoryAccount() {
        var expense = stage("r1", CASH, FOOD, "-120.50");

        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(1, 0, 0, 0));
        var transaction = transactionOf(expense);
        assertThat(journalsOf(transaction)).containsExactlyInAnyOrder(
                credit(cashId, "120.5"), debit(groceriesId, "120.5"));
        assertThat(transaction.fxRate()).isNull();
    }

    @Test
    void shouldPostIncomeAsDebitOfAccountAndCreditOfCategoryAccount() {
        var income = stage("r1", CASH, SALARY, "50000");

        postingService.post(connectionId);

        assertThat(journalsOf(transactionOf(income))).containsExactlyInAnyOrder(
                debit(cashId, "50000"), credit(salaryId, "50000"));
    }

    @Test
    void shouldDescribeTransactionByCounterpartyAndNoteAndDateJournalsByRecordDate() {
        var expense = stage("r1", CASH, FOOD, "-100", importRecord -> {
            importRecord.setCounterparty(" Bistro ");
            importRecord.setNote("Lunch");
        });

        postingService.post(connectionId);

        var transaction = transactionOf(expense);
        assertThat(transaction.description()).isEqualTo("Bistro - Lunch");
        assertThat(transaction.journals()).extracting(AccJournalDto::date, AccJournalDto::description)
                .containsOnly(tuple(DATE, "Bistro - Lunch"));
    }

    @Test
    void shouldLinkPostedTransactionToItsSourceRecord() {
        var expense = stage("r1", CASH, FOOD, "-100");

        postingService.post(connectionId);

        var posted = reload(expense);
        assertThat(posted.getStatus()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(posted.getPostedHash()).hasSize(64);
        assertThat(transactionService.getTransaction(posted.getTransactionId()))
                .returns(connectionId, AccTransactionDto::sourceConnectionId)
                .returns("r1", AccTransactionDto::sourceExternalId);
    }

    @Test
    void shouldPostBothLegsOfTransferAsOneTransaction() {
        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        var incoming = stage("t-in", BANK, null, "500", transferLeg());

        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(1, 0, 0, 0));
        var transaction = transactionOf(outgoing);
        assertThat(reload(incoming).getTransactionId()).isEqualTo(transaction.id());
        assertThat(reload(incoming).getStatus()).isEqualTo(ImportRecordStatus.POSTED);
        assertThat(journalsOf(transaction)).containsExactlyInAnyOrder(credit(cashId, "500"), debit(bankId, "500"));
        assertThat(transaction.sourceExternalId()).isEqualTo("t-out");
        assertThat(transactionService.getAllTransactions()).hasSize(1);
    }

    @Test
    void shouldKeepTransferLegWaitingUntilOtherLegIsStaged() {
        var incoming = stage("t-in", BANK, null, "500", transferLeg());

        assertThat(postingService.post(connectionId)).isEqualTo(PostingResult.EMPTY);
        assertThat(reload(incoming).getStatus()).isEqualTo(ImportRecordStatus.NEW);

        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        assertThat(postingService.post(connectionId)).isEqualTo(new PostingResult(1, 0, 0, 0));
        assertThat(reload(incoming).getTransactionId()).isEqualTo(reload(outgoing).getTransactionId()).isNotNull();
    }

    @Test
    void shouldSetFxRateFromBaseAmountWhenRecordIsInForeignCurrency() {
        currencyService.createCurrency(new AccCurrencyCreateRequestDto("EUR", "Euro", false, 2));
        var cardId = map(CARD, createAccount("Card", AccAcountType.ASSET, "EUR"));
        var expense = stage("r1", CARD, FOOD, "-100", importRecord -> {
            importRecord.setCurrencyCode("EUR");
            importRecord.setBaseAmount(new BigDecimal("-2512.34"));
            importRecord.setBaseCurrencyCode("CZK");
        });

        postingService.post(connectionId);

        var transaction = transactionOf(expense);
        assertThat(transaction.fxRate()).isEqualByComparingTo("25.1234");
        assertThat(journalsOf(transaction)).containsExactlyInAnyOrder(credit(cardId, "100"), debit(groceriesId, "100"));
    }

    @Test
    void shouldRewriteJournalsOfTransactionWhenSourceRecordChanged() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        var transactionId = reload(expense).getTransactionId();

        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-250")));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(1, 0, 0, 0));
        assertThat(reload(expense).getTransactionId()).isEqualTo(transactionId);
        assertThat(journalsOf(transactionService.getTransaction(transactionId)))
                .containsExactlyInAnyOrder(credit(cashId, "250"), debit(groceriesId, "250"));
        assertThat(transactionService.getAllTransactions()).hasSize(1);
    }

    @Test
    void shouldDeleteTransactionWhenRecordIsDeletedInSource() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        var transactionId = reload(expense).getTransactionId();

        changeInSource(expense, importRecord -> importRecord.setStatus(ImportRecordStatus.DELETED));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(0, 1, 0, 0));
        assertThatThrownBy(() -> transactionService.getTransaction(transactionId))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(reload(expense))
                .returns(ImportRecordStatus.DELETED, AccImportRecordEntity::getStatus)
                .returns(null, AccImportRecordEntity::getTransactionId)
                .returns(null, AccImportRecordEntity::getPostedHash);
    }

    @Test
    void shouldReturnOtherLegOfTransferToNewWhenOneLegIsDeleted() {
        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        var incoming = stage("t-in", BANK, null, "500", transferLeg());
        postingService.post(connectionId);

        changeInSource(outgoing, importRecord -> importRecord.setStatus(ImportRecordStatus.DELETED));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(0, 1, 0, 0));
        assertThat(transactionService.getAllTransactions()).isEmpty();
        assertThat(reload(incoming))
                .returns(ImportRecordStatus.NEW, AccImportRecordEntity::getStatus)
                .returns(null, AccImportRecordEntity::getTransactionId);
    }

    @Test
    void shouldMarkConflictInsteadOfOverwritingTransactionChangedInFrp() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        var transactionId = reload(expense).getTransactionId();
        editInFrp(transactionId, "Edited by hand");

        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-250")));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(0, 0, 1, 0));
        assertThat(reload(expense))
                .returns(ImportRecordStatus.CONFLICT, AccImportRecordEntity::getStatus)
                .returns("Transaction was changed in FRP", AccImportRecordEntity::getErrorMessage);
        var transaction = transactionService.getTransaction(transactionId);
        assertThat(transaction.description()).isEqualTo("Edited by hand");
        assertThat(journalsOf(transaction)).containsExactlyInAnyOrder(credit(cashId, "100"), debit(groceriesId, "100"));
    }

    @Test
    void shouldMarkConflictInsteadOfDeletingTransactionChangedInFrp() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        var transactionId = reload(expense).getTransactionId();
        editInFrp(transactionId, "Edited by hand");

        changeInSource(expense, importRecord -> importRecord.setStatus(ImportRecordStatus.DELETED));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(0, 0, 1, 0));
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.CONFLICT);
        assertThat(transactionService.getTransaction(transactionId).description()).isEqualTo("Edited by hand");
    }

    @Test
    void shouldMarkConflictInsteadOfRecreatingTransactionDeletedInFrp() {
        var expense = stage("r1", CASH, FOOD, "-100");
        postingService.post(connectionId);
        transactionService.deleteTransaction(reload(expense).getTransactionId());

        changeInSource(expense, importRecord -> importRecord.setAmount(new BigDecimal("-250")));
        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(0, 0, 1, 0));
        assertThat(reload(expense))
                .returns(ImportRecordStatus.CONFLICT, AccImportRecordEntity::getStatus)
                .returns("Transaction was deleted in FRP", AccImportRecordEntity::getErrorMessage);
        assertThat(transactionService.getAllTransactions()).isEmpty();
    }

    @Test
    void shouldMarkFailingRecordAsErrorAndPostTheOthers() {
        var zero = stage("r1", CASH, FOOD, "0");
        var expense = stage("r2", CASH, FOOD, "-100");

        var result = postingService.post(connectionId);

        assertThat(result).isEqualTo(new PostingResult(1, 0, 0, 1));
        assertThat(reload(zero))
                .returns(ImportRecordStatus.ERROR, AccImportRecordEntity::getStatus)
                .returns("Record r1 has a zero amount", AccImportRecordEntity::getErrorMessage);
        assertThat(reload(expense).getStatus()).isEqualTo(ImportRecordStatus.POSTED);
    }

    @Test
    void shouldFailTransferBetweenDifferentCurrencies() {
        currencyService.createCurrency(new AccCurrencyCreateRequestDto("EUR", "Euro", false, 2));
        map(CARD, createAccount("Card", AccAcountType.ASSET, "EUR"));
        var outgoing = stage("t-out", CASH, null, "-2500", transferLeg());
        stage("t-in", CARD, null, "100", transferLeg().andThen(importRecord -> importRecord.setCurrencyCode("EUR")));

        var result = postingService.post(connectionId);

        assertThat(result.failed()).isEqualTo(2);
        assertThat(reload(outgoing))
                .returns(ImportRecordStatus.ERROR, AccImportRecordEntity::getStatus)
                .returns("Transfer between currencies CZK and EUR is not supported",
                        AccImportRecordEntity::getErrorMessage);
    }

    @Test
    void shouldFailTransferWhoseLegAmountsDoNotMatch() {
        var outgoing = stage("t-out", CASH, null, "-500", transferLeg());
        stage("t-in", BANK, null, "490", transferLeg());

        postingService.post(connectionId);

        assertThat(reload(outgoing))
                .returns(ImportRecordStatus.ERROR, AccImportRecordEntity::getStatus)
                .returns("Amounts of transfer t-1 do not match", AccImportRecordEntity::getErrorMessage);
    }

}
