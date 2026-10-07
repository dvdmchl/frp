package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccTransactionDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

// shares the application context (and its fake connector) with ExternalMappingServiceTest
@Import(ExternalMappingServiceTest.MappingTestConfig.class)
class ImportPostingServiceTest extends AbstractDbTest {

    private static final String SCHEMA = "posting_tenant";
    private static final String TYPE = "FAKE_MAPPING";
    private static final String CASH = "acc-cash";
    private static final String BANK = "acc-bank";
    private static final String CARD = "acc-card";
    private static final String FOOD = "cat-food";
    private static final String SALARY = "cat-salary";
    private static final String TRANSFER = "t-1";
    private static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    @Autowired
    private ImportPostingService postingService;
    @Autowired
    private ExternalMappingService mappingService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CurrencyService currencyService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccImportRecordRepository importRecordRepository;
    @Autowired
    private FakeAccountingConnector connector;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SecurityContextService securityContextService;
    @Autowired
    private TenantUtil tenantUtil;

    private Long connectionId;
    private Long cashId;
    private Long bankId;
    private Long groceriesId;
    private Long salaryId;

    @BeforeEach
    void setUp() {
        var user = new UserEntity();
        user.setEmail("posting@test.com");
        user.setPassword("password");
        var owner = userRepository.save(user);
        owner.setSchema(schemaService.createSchema(SCHEMA, owner.getId()));
        owner = userRepository.save(owner);

        securityContextService.setAuthentication(new TestingAuthenticationToken(owner, null, List.of()));
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());

        connector.reset();
        connector.setAccounts(List.of(new ExternalAccount(CASH, "Cash", "CZK", "{}"),
                new ExternalAccount(BANK, "Bank", "CZK", "{}"), new ExternalAccount(CARD, "Card", "EUR", "{}")));
        connector.setCategories(List.of(new ExternalCategory(FOOD, "Food", null, "{}"),
                new ExternalCategory(SALARY, "Salary", null, "{}")));
        connectionId = connectionService.createConnection(new AccConnectionCreateRequestDto(TYPE, "Wallet",
                Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN), Map.of())).id();
        mappingService.refreshMappings(connectionId);
        cashId = map(CASH, createAccount("Wallet cash", AccAcountType.ASSET, "CZK"));
        bankId = map(BANK, createAccount("Bank account", AccAcountType.ASSET, "CZK"));
        groceriesId = map(FOOD, createAccount("Groceries", AccAcountType.EXPENSE, "CZK"));
        salaryId = map(SALARY, createAccount("Salary", AccAcountType.REVENUE, "CZK"));
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        securityContextService.clearContext();
    }

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

    private Long createAccount(String name, AccAcountType type, String currencyCode) {
        return accountService.createAccount(new AccAccountCreateRequestDto(null, name, null, currencyCode,
                type == AccAcountType.ASSET, type, false)).account().id();
    }

    private Long map(String externalId, Long accountId) {
        var mappingId = mappingService.getMappings(connectionId).stream()
                .filter(mapping -> mapping.externalId().equals(externalId))
                .findFirst().orElseThrow().id();
        mappingService.updateMapping(mappingId, new AccExternalMappingUpdateRequestDto(accountId, false));
        return accountId;
    }

    private AccImportRecordEntity stage(String externalId, String externalAccountId, String externalCategoryId,
                                        String amount) {
        return stage(externalId, externalAccountId, externalCategoryId, amount, importRecord -> { });
    }

    private AccImportRecordEntity stage(String externalId, String externalAccountId, String externalCategoryId,
                                        String amount, Consumer<AccImportRecordEntity> customizer) {
        var importRecord = new AccImportRecordEntity();
        importRecord.setConnectionId(connectionId);
        importRecord.setExternalId(externalId);
        importRecord.setExternalAccountId(externalAccountId);
        importRecord.setExternalCategoryId(externalCategoryId);
        importRecord.setRecordDate(DATE);
        importRecord.setAmount(new BigDecimal(amount));
        importRecord.setCurrencyCode("CZK");
        importRecord.setSourceState(ExternalRecordState.BOOKED);
        importRecord.setPayloadHash(externalId);
        importRecord.setStatus(ImportRecordStatus.NEW);
        importRecord.setFirstSeenAt(Instant.parse("2026-01-15T10:00:00Z"));
        importRecord.setLastSeenAt(Instant.parse("2026-01-15T10:00:00Z"));
        customizer.accept(importRecord);
        return importRecordRepository.save(importRecord);
    }

    private static Consumer<AccImportRecordEntity> transferLeg() {
        return importRecord -> importRecord.setTransferLinkId(TRANSFER);
    }

    /**
     * Applies a change as the staging does when the source record changed: the record goes back to NEW.
     */
    private void changeInSource(AccImportRecordEntity importRecord, Consumer<AccImportRecordEntity> change) {
        var changed = reload(importRecord);
        changed.setStatus(ImportRecordStatus.NEW);
        change.accept(changed);
        importRecordRepository.save(changed);
    }

    private void editInFrp(Long transactionId, String description) {
        var transaction = transactionService.getTransaction(transactionId);
        var journals = transaction.journals().stream()
                .map(journal -> new AccJournalCreateRequestDto(journal.date(), journal.description(),
                        journal.accountId(), journal.credit(), journal.debit()))
                .toList();
        transactionService.updateTransaction(transactionId, new AccTransactionCreateRequestDto(
                transaction.reference(), description, transaction.fxRate(), journals));
    }

    private AccImportRecordEntity reload(AccImportRecordEntity importRecord) {
        return importRecordRepository.findById(importRecord.getId()).orElseThrow();
    }

    private AccTransactionDto transactionOf(AccImportRecordEntity importRecord) {
        return transactionService.getTransaction(reload(importRecord).getTransactionId());
    }

    private static List<String> journalsOf(AccTransactionDto transaction) {
        return transaction.journals().stream()
                .map(journal -> journal.credit().signum() > 0
                        ? credit(journal.accountId(), journal.credit().stripTrailingZeros().toPlainString())
                        : debit(journal.accountId(), journal.debit().stripTrailingZeros().toPlainString()))
                .toList();
    }

    private static String credit(Long accountId, String amount) {
        return "credit " + amount + " to " + accountId;
    }

    private static String debit(Long accountId, String amount) {
        return "debit " + amount + " to " + accountId;
    }
}
