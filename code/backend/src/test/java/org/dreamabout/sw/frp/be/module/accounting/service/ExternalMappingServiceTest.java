package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAccount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalCategory;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.ExternalMappingKind;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccAccountEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionFallbackRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccCurrencyCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccAccountRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccNodeRepository;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@Import(ExternalMappingServiceTest.MappingTestConfig.class)
class ExternalMappingServiceTest extends AbstractDbTest {

    private static final String SCHEMA = "mapping_tenant";
    private static final String TYPE = "FAKE_MAPPING";
    private static final String CASH = "acc-cash";
    private static final String CARD = "acc-card";
    private static final String FOOD = "cat-food";
    private static final String SALARY = "cat-salary";

    @TestConfiguration
    static class MappingTestConfig {
        @Bean
        FakeAccountingConnector mappingFakeConnector() {
            return new FakeAccountingConnector(TYPE, List.of());
        }
    }

    @Autowired
    private ExternalMappingService mappingService;
    @Autowired
    private RecordMappingService recordMappingService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private CurrencyService currencyService;
    @Autowired
    private AccAccountRepository accountRepository;
    @Autowired
    private AccNodeRepository nodeRepository;
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

    @BeforeEach
    void setUp() {
        var user = new UserEntity();
        user.setEmail("mapping@test.com");
        user.setPassword("password");
        var owner = userRepository.save(user);
        owner.setSchema(schemaService.createSchema(SCHEMA, owner.getId()));
        owner = userRepository.save(owner);

        securityContextService.setAuthentication(new TestingAuthenticationToken(owner, null, List.of()));
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());

        connector.reset();
        connector.setAccounts(List.of(new ExternalAccount(CASH, "Cash", "CZK", "{}"),
                new ExternalAccount(CARD, "Card", "EUR", "{}")));
        connector.setCategories(List.of(new ExternalCategory(FOOD, "Food", null, "{}"),
                new ExternalCategory(SALARY, "Salary", null, "{}")));
        connectionId = connectionService.createConnection(new AccConnectionCreateRequestDto(TYPE, "Wallet",
                Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN), Map.of())).id();
        mappingService.refreshMappings(connectionId);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        securityContextService.clearContext();
    }

    @Test
    void shouldAddExternalAccountsAndCategoriesUnmappedWhenRefreshed() {
        var mappings = mappingService.refreshMappings(connectionId);

        assertThat(mappings)
                .extracting(AccExternalMappingDto::kind, AccExternalMappingDto::externalId,
                        AccExternalMappingDto::externalName, AccExternalMappingDto::currencyCode,
                        AccExternalMappingDto::accountId, AccExternalMappingDto::ignored)
                .containsExactly(
                        tuple(ExternalMappingKind.ACCOUNT, CARD, "Card", "EUR", null, false),
                        tuple(ExternalMappingKind.ACCOUNT, CASH, "Cash", "CZK", null, false),
                        tuple(ExternalMappingKind.CATEGORY, FOOD, "Food", null, null, false),
                        tuple(ExternalMappingKind.CATEGORY, SALARY, "Salary", null, null, false));
    }

    @Test
    void shouldKeepMappingAndUpdateNameWhenRefreshedAgain() {
        var cashAccountId = createAccount("Wallet cash", AccAcountType.ASSET);
        mappingService.updateMapping(connectionId, mappingId(CASH), new AccExternalMappingUpdateRequestDto(cashAccountId, false));
        connector.setAccounts(List.of(new ExternalAccount(CASH, "Pocket money", "CZK", "{}")));
        connector.setCategories(List.of());

        var mappings = mappingService.refreshMappings(connectionId);

        assertThat(mappings).hasSize(4)
                .filteredOn(mapping -> mapping.externalId().equals(CASH))
                .singleElement()
                .returns("Pocket money", AccExternalMappingDto::externalName)
                .returns(cashAccountId, AccExternalMappingDto::accountId);
    }

    @Test
    void shouldMapExternalAccountToAssetAccount() {
        var cashAccountId = createAccount("Wallet cash", AccAcountType.ASSET);

        var mapping = mappingService.updateMapping(connectionId, mappingId(CASH),
                new AccExternalMappingUpdateRequestDto(cashAccountId, false));

        assertThat(mapping.accountId()).isEqualTo(cashAccountId);
        assertThat(mappingService.getMappings(connectionId))
                .filteredOn(dto -> dto.externalId().equals(CASH))
                .extracting(AccExternalMappingDto::accountId)
                .containsExactly(cashAccountId);
    }

    @Test
    void shouldRejectMappingExternalAccountToExpenseAccount() {
        var expenseId = createAccount("Groceries", AccAcountType.EXPENSE);
        var mappingId = mappingId(CASH);
        var request = new AccExternalMappingUpdateRequestDto(expenseId, false);

        assertThatThrownBy(() -> mappingService.updateMapping(connectionId, mappingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account Groceries must be of type [ASSET, LIABILITY]");
    }

    @Test
    void shouldRejectMappingCategoryToPlaceholderAccount() {
        var placeholder = accountService.createAccount(new AccAccountCreateRequestDto(null, "Expenses", null, "CZK",
                false, AccAcountType.EXPENSE, true)).account().id();
        var mappingId = mappingId(FOOD);
        var request = new AccExternalMappingUpdateRequestDto(placeholder, false);

        assertThatThrownBy(() -> mappingService.updateMapping(connectionId, mappingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account Expenses is a placeholder");
    }

    @Test
    void shouldRejectUpdateOfMappingOfOtherConnection() {
        var otherConnectionId = connectionService.createConnection(new AccConnectionCreateRequestDto(TYPE, "Bank",
                Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN), Map.of())).id();
        var mappingId = mappingId(CASH);
        var request = new AccExternalMappingUpdateRequestDto(null, true);

        assertThatThrownBy(() -> mappingService.updateMapping(otherConnectionId, mappingId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mapping not found");
        assertThat(mappingService.getMappings(connectionId))
                .filteredOn(dto -> dto.externalId().equals(CASH))
                .extracting(AccExternalMappingDto::ignored)
                .containsExactly(false);
    }

    @Test
    void shouldSetFallbackAccountsOfConnection() {
        var expenseId = createAccount("Uncategorized expense", AccAcountType.EXPENSE);
        var revenueId = createAccount("Uncategorized income", AccAcountType.REVENUE);

        var connection = connectionService.setFallbackAccounts(connectionId,
                new AccConnectionFallbackRequestDto(expenseId, revenueId));

        assertThat(connection.fallbackExpenseAccountId()).isEqualTo(expenseId);
        assertThat(connection.fallbackRevenueAccountId()).isEqualTo(revenueId);
    }

    @Test
    void shouldRejectRevenueAccountAsExpenseFallback() {
        var revenueId = createAccount("Uncategorized income", AccAcountType.REVENUE);
        var request = new AccConnectionFallbackRequestDto(revenueId, null);

        assertThatThrownBy(() -> connectionService.setFallbackAccounts(connectionId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account Uncategorized income must be of type [EXPENSE]");
    }

    @Test
    void shouldCreateMissingAssetAccountsInSourceCurrencyAndResolveNameConflicts() {
        currencyService.createCurrency(new AccCurrencyCreateRequestDto("EUR", "Euro", false, 2));
        createAccount("Cash", AccAcountType.ASSET);
        var parentNodeId = accountService.createAccount(new AccAccountCreateRequestDto(null, "Assets", null, "CZK",
                false, AccAcountType.ASSET, true)).id();

        var created = mappingService.createMissingAccounts(connectionId, ExternalMappingKind.ACCOUNT, parentNodeId);

        assertThat(created).hasSize(2).allSatisfy(mapping -> assertThat(mapping.accountId()).isNotNull());
        assertThat(created).extracting(mapping -> describe(mapping.accountId()))
                .containsExactlyInAnyOrder("Cash (2)/ASSET/CZK", "Card/ASSET/EUR");
        assertThat(created).extracting(mapping -> nodeRepository.findByAccountId(mapping.accountId()).orElseThrow()
                        .getParent().getId())
                .containsOnly(parentNodeId);
    }

    @Test
    void shouldCreateCategoryAccountsByNetAmountInBaseCurrency() {
        stage("r1", CASH, FOOD, "-120");
        stage("r2", CASH, FOOD, "20");
        stage("r3", CASH, SALARY, "50000");

        var created = mappingService.createMissingAccounts(connectionId, ExternalMappingKind.CATEGORY, null);

        assertThat(created).extracting(mapping -> describe(mapping.accountId()))
                .containsExactlyInAnyOrder("Food/EXPENSE/CZK", "Salary/REVENUE/CZK");
    }

    @Test
    void shouldNotCreateAccountsForIgnoredOrMappedExternalAccounts() {
        mappingService.updateMapping(connectionId, mappingId(CARD), new AccExternalMappingUpdateRequestDto(null, true));
        var cashAccountId = createAccount("Wallet cash", AccAcountType.ASSET);
        mappingService.updateMapping(connectionId, mappingId(CASH), new AccExternalMappingUpdateRequestDto(cashAccountId, false));

        assertThat(mappingService.createMissingAccounts(connectionId, ExternalMappingKind.ACCOUNT, null)).isEmpty();
    }

    @Test
    void shouldFailCreatingAccountWhenSourceCurrencyDoesNotExist() {

        assertThatThrownBy(() -> mappingService.createMissingAccounts(connectionId, ExternalMappingKind.ACCOUNT, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency not found: EUR");
    }

    @Test
    void shouldKeepRecordNewAndUnpostedWhenExternalAccountIsUnmapped() {
        map(FOOD, createAccount("Groceries", AccAcountType.EXPENSE));
        var importRecord = stage("r1", CASH, FOOD, "-100");

        var mapped = recordMappingService.applyMappings(connectionId);

        assertThat(mapped).isEmpty();
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldReturnAccountsOfMappedRecord() {
        var cashAccountId = map(CASH, createAccount("Wallet cash", AccAcountType.ASSET));
        var groceriesId = map(FOOD, createAccount("Groceries", AccAcountType.EXPENSE));
        var importRecord = stage("r1", CASH, FOOD, "-100");

        var mapped = recordMappingService.applyMappings(connectionId);

        assertThat(mapped).containsExactly(new MappedRecord(importRecord.getId(), cashAccountId, groceriesId));
    }

    @Test
    void shouldUseFallbackAccountByAmountSignWhenCategoryIsUnmapped() {
        var cashAccountId = map(CASH, createAccount("Wallet cash", AccAcountType.ASSET));
        var expenseId = createAccount("Uncategorized expense", AccAcountType.EXPENSE);
        var revenueId = createAccount("Uncategorized income", AccAcountType.REVENUE);
        connectionService.setFallbackAccounts(connectionId, new AccConnectionFallbackRequestDto(expenseId, revenueId));
        var outgoing = stage("r1", CASH, FOOD, "-100");
        var incoming = stage("r2", CASH, null, "300");

        var mapped = recordMappingService.applyMappings(connectionId);

        assertThat(mapped).containsExactlyInAnyOrder(
                new MappedRecord(outgoing.getId(), cashAccountId, expenseId),
                new MappedRecord(incoming.getId(), cashAccountId, revenueId));
    }

    @Test
    void shouldKeepRecordNewWhenCategoryIsUnmappedWithoutFallback() {
        map(CASH, createAccount("Wallet cash", AccAcountType.ASSET));
        var importRecord = stage("r1", CASH, FOOD, "-100");

        assertThat(recordMappingService.applyMappings(connectionId)).isEmpty();
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldNotNeedCategoryForTransferBetweenOwnAccounts() {
        var cashAccountId = map(CASH, createAccount("Wallet cash", AccAcountType.ASSET));
        var transfer = stage("r1", CASH, FOOD, "-100");
        transfer.setTransferLinkId("t-1");
        transfer = importRecordRepository.save(transfer);

        var mapped = recordMappingService.applyMappings(connectionId);

        assertThat(mapped).containsExactly(new MappedRecord(transfer.getId(), cashAccountId, null));
    }

    @Test
    void shouldMarkRecordAsErrorUntilItsCurrencyIsAdded() {
        var cardAccountId = map(CARD, createAccount("Wallet card", AccAcountType.LIABILITY));
        var groceriesId = map(FOOD, createAccount("Groceries", AccAcountType.EXPENSE));
        var importRecord = stage("r1", CARD, FOOD, "-10");
        importRecord.setCurrencyCode("EUR");
        importRecord = importRecordRepository.save(importRecord);

        assertThat(recordMappingService.applyMappings(connectionId)).isEmpty();
        assertThat(importRecordRepository.findById(importRecord.getId()).orElseThrow())
                .returns(ImportRecordStatus.ERROR, AccImportRecordEntity::getStatus)
                .returns("Currency EUR does not exist", AccImportRecordEntity::getErrorMessage);

        currencyService.createCurrency(new AccCurrencyCreateRequestDto("EUR", "Euro", false, 2));
        var mapped = recordMappingService.applyMappings(connectionId);

        assertThat(mapped).containsExactly(new MappedRecord(importRecord.getId(), cardAccountId, groceriesId));
        assertThat(importRecordRepository.findById(importRecord.getId()).orElseThrow())
                .returns(ImportRecordStatus.NEW, AccImportRecordEntity::getStatus)
                .returns(null, AccImportRecordEntity::getErrorMessage);
    }

    @Test
    void shouldSkipRecordsOfIgnoredAccountAndReturnThemWhenNoLongerIgnored() {
        var importRecord = stage("r1", CARD, FOOD, "-10");

        mappingService.updateMapping(connectionId, mappingId(CARD), new AccExternalMappingUpdateRequestDto(null, true));
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.SKIPPED);

        mappingService.updateMapping(connectionId, mappingId(CARD), new AccExternalMappingUpdateRequestDto(null, false));
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldSkipRecordsOfIgnoredCategoryAndReturnThemWhenNoLongerIgnored() {
        var importRecord = stage("r1", CASH, SALARY, "1000");

        mappingService.updateMapping(connectionId, mappingId(SALARY), new AccExternalMappingUpdateRequestDto(null, true));
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.SKIPPED);

        mappingService.updateMapping(connectionId, mappingId(SALARY), new AccExternalMappingUpdateRequestDto(null, false));
        assertThat(statusOf(importRecord)).isEqualTo(ImportRecordStatus.NEW);
    }

    private Long createAccount(String name, AccAcountType type) {
        return accountService.createAccount(new AccAccountCreateRequestDto(null, name, null, "CZK",
                type == AccAcountType.ASSET, type, false)).account().id();
    }

    private Long mappingId(String externalId) {
        return mappingService.getMappings(connectionId).stream()
                .filter(mapping -> mapping.externalId().equals(externalId))
                .findFirst().orElseThrow().id();
    }

    private Long map(String externalId, Long accountId) {
        mappingService.updateMapping(connectionId, mappingId(externalId), new AccExternalMappingUpdateRequestDto(accountId, false));
        return accountId;
    }

    private String describe(Long accountId) {
        AccAccountEntity account = accountRepository.findById(accountId).orElseThrow();
        return account.getName() + "/" + account.getAccountType() + "/" + account.getCurrency().getCode();
    }

    private AccImportRecordEntity stage(String externalId, String externalAccountId, String externalCategoryId,
                                        String amount) {
        var importRecord = new AccImportRecordEntity();
        importRecord.setConnectionId(connectionId);
        importRecord.setExternalId(externalId);
        importRecord.setExternalAccountId(externalAccountId);
        importRecord.setExternalCategoryId(externalCategoryId);
        importRecord.setRecordDate(LocalDate.of(2026, 1, 15));
        importRecord.setAmount(new BigDecimal(amount));
        importRecord.setCurrencyCode("CZK");
        importRecord.setSourceState(ExternalRecordState.BOOKED);
        importRecord.setPayloadHash(externalId);
        importRecord.setStatus(ImportRecordStatus.NEW);
        importRecord.setFirstSeenAt(Instant.parse("2026-01-15T10:00:00Z"));
        importRecord.setLastSeenAt(Instant.parse("2026-01-15T10:00:00Z"));
        return importRecordRepository.save(importRecord);
    }

    private ImportRecordStatus statusOf(AccImportRecordEntity importRecord) {
        return importRecordRepository.findById(importRecord.getId()).orElseThrow().getStatus();
    }
}
