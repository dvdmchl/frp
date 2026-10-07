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
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccJournalCreateRequestDto;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Tenant with a connection whose external accounts and categories are mapped, and helpers to stage records and to
 * change their transactions in FRP. Shares the application context (and its fake connector) with
 * ExternalMappingServiceTest.
 */
@Import(ExternalMappingServiceTest.MappingTestConfig.class)
abstract class AbstractImportPostingTest extends AbstractDbTest {

    private static final String SCHEMA = "posting_tenant";
    private static final String TYPE = "FAKE_MAPPING";
    protected static final String CASH = "acc-cash";
    protected static final String BANK = "acc-bank";
    protected static final String CARD = "acc-card";
    protected static final String FOOD = "cat-food";
    protected static final String SALARY = "cat-salary";
    private static final String TRANSFER = "t-1";
    protected static final LocalDate DATE = LocalDate.of(2026, 1, 15);

    @Autowired
    protected ImportPostingService postingService;
    @Autowired
    private ExternalMappingService mappingService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AccountService accountService;
    @Autowired
    protected CurrencyService currencyService;
    @Autowired
    protected TransactionService transactionService;
    @Autowired
    protected AccImportRecordRepository importRecordRepository;
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

    protected Long connectionId;
    protected Long cashId;
    protected Long bankId;
    protected Long groceriesId;
    protected Long salaryId;

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

    protected Long createAccount(String name, AccAcountType type, String currencyCode) {
        return accountService.createAccount(new AccAccountCreateRequestDto(null, name, null, currencyCode,
                type == AccAcountType.ASSET, type, false)).account().id();
    }

    protected Long map(String externalId, Long accountId) {
        var mappingId = mappingService.getMappings(connectionId).stream()
                .filter(mapping -> mapping.externalId().equals(externalId))
                .findFirst().orElseThrow().id();
        mappingService.updateMapping(connectionId, mappingId, new AccExternalMappingUpdateRequestDto(accountId, false));
        return accountId;
    }

    protected AccImportRecordEntity stage(String externalId, String externalAccountId, String externalCategoryId,
                                        String amount) {
        return stage(externalId, externalAccountId, externalCategoryId, amount, importRecord -> { });
    }

    protected AccImportRecordEntity stage(String externalId, String externalAccountId, String externalCategoryId,
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

    protected static Consumer<AccImportRecordEntity> transferLeg() {
        return importRecord -> importRecord.setTransferLinkId(TRANSFER);
    }

    /**
     * Applies a change as the staging does when the source record changed: the record goes back to NEW.
     */
    protected void changeInSource(AccImportRecordEntity importRecord, Consumer<AccImportRecordEntity> change) {
        var changed = reload(importRecord);
        changed.setStatus(ImportRecordStatus.NEW);
        change.accept(changed);
        importRecordRepository.save(changed);
    }

    protected void editInFrp(Long transactionId, String description) {
        var transaction = transactionService.getTransaction(transactionId);
        var journals = transaction.journals().stream()
                .map(journal -> new AccJournalCreateRequestDto(journal.date(), journal.description(),
                        journal.accountId(), journal.credit(), journal.debit()))
                .toList();
        transactionService.updateTransaction(transactionId, new AccTransactionCreateRequestDto(
                transaction.reference(), description, transaction.fxRate(), journals));
    }

    protected AccImportRecordEntity reload(AccImportRecordEntity importRecord) {
        return importRecordRepository.findById(importRecord.getId()).orElseThrow();
    }

    protected AccTransactionDto transactionOf(AccImportRecordEntity importRecord) {
        return transactionService.getTransaction(reload(importRecord).getTransactionId());
    }

    protected static List<String> journalsOf(AccTransactionDto transaction) {
        return transaction.journals().stream()
                .map(journal -> journal.credit().signum() > 0
                        ? credit(journal.accountId(), journal.credit().stripTrailingZeros().toPlainString())
                        : debit(journal.accountId(), journal.debit().stripTrailingZeros().toPlainString()))
                .toList();
    }

    protected static String credit(Long accountId, String amount) {
        return "credit " + amount + " to " + accountId;
    }

    protected static String debit(Long accountId, String amount) {
        return "debit " + amount + " to " + accountId;
    }
}
