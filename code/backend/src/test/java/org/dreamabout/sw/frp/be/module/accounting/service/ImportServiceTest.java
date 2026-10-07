package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorTransientException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalAmount;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecord;
import org.dreamabout.sw.frp.be.module.accounting.connector.ExternalRecordState;
import org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.connector.RecordWindow;
import org.dreamabout.sw.frp.be.module.accounting.domain.ImportRecordStatus;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccImportRecordEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccImportRecordRepository;
import org.dreamabout.sw.frp.be.module.common.model.UserEntity;
import org.dreamabout.sw.frp.be.module.common.repository.UserRepository;
import org.dreamabout.sw.frp.be.module.common.service.SchemaService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.dreamabout.sw.frp.be.test.MutableClock;
import org.dreamabout.sw.multitenancy.core.TenantContext;
import org.dreamabout.sw.multitenancy.core.TenantIdentifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector.bookedRecord;

@Import(ImportServiceTest.ImportTestConfig.class)
class ImportServiceTest extends AbstractDbTest {

    private static final String SCHEMA = "import_tenant";
    private static final String TYPE = "FAKE_IMPORT";
    private static final Instant NOW = Instant.parse("2026-02-01T08:00:00Z");

    @TestConfiguration
    static class ImportTestConfig {
        @Bean
        FakeAccountingConnector importFakeConnector() {
            return new FakeAccountingConnector(TYPE, List.of());
        }

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(NOW);
        }
    }

    @Autowired
    private ImportService importService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private AccImportRecordRepository importRecordRepository;
    @Autowired
    private AccConnectionRepository connectionRepository;
    @Autowired
    private FakeAccountingConnector connector;
    @Autowired
    private MutableClock clock;
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
        user.setEmail("import@test.com");
        user.setPassword("password");
        var owner = userRepository.save(user);
        owner.setSchema(schemaService.createSchema(SCHEMA, owner.getId()));
        owner = userRepository.save(owner);

        securityContextService.setAuthentication(new TestingAuthenticationToken(owner, null, List.of()));
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());

        connector.reset();
        clock.setInstant(NOW);
        connectionId = connectionService.createConnection(new AccConnectionCreateRequestDto(TYPE, "Wallet",
                Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN), Map.of())).id();
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
        securityContextService.clearContext();
    }

    @Test
    void shouldStageRecordsOfAllPagesAsNew() {
        var externalRecord = ExternalRecord.builder()
                .externalId("r1")
                .externalAccountId("acc-1")
                .date(LocalDate.of(2026, 1, 20))
                .amount(new ExternalAmount(new BigDecimal("-123.45"), "EUR"))
                .baseAmount(new ExternalAmount(new BigDecimal("-3086.25"), "CZK"))
                .externalCategoryId("cat-1")
                .note("Lunch")
                .counterparty("Bistro")
                .state(ExternalRecordState.PENDING)
                .transferLinkId("t-1")
                .updatedAt(Instant.parse("2026-01-20T12:00:00Z"))
                .rawPayload("{\"id\": \"r1\"}")
                .build();
        connector.setRecords(List.of(externalRecord, bookedRecord("r2", "50")));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(2, 2, 0, 0));
        assertThat(importRecordRepository.findByConnectionIdAndExternalId(connectionId, "r1").orElseThrow())
                .returns("acc-1", AccImportRecordEntity::getExternalAccountId)
                .returns(LocalDate.of(2026, 1, 20), AccImportRecordEntity::getRecordDate)
                .returns("EUR", AccImportRecordEntity::getCurrencyCode)
                .returns("cat-1", AccImportRecordEntity::getExternalCategoryId)
                .returns("Lunch", AccImportRecordEntity::getNote)
                .returns("Bistro", AccImportRecordEntity::getCounterparty)
                .returns(ExternalRecordState.PENDING, AccImportRecordEntity::getSourceState)
                .returns("t-1", AccImportRecordEntity::getTransferLinkId)
                .returns(Instant.parse("2026-01-20T12:00:00Z"), AccImportRecordEntity::getSourceUpdatedAt)
                .returns(ImportRecordStatus.NEW, AccImportRecordEntity::getStatus)
                .returns(NOW, AccImportRecordEntity::getFirstSeenAt)
                .returns(NOW, AccImportRecordEntity::getLastSeenAt)
                .satisfies(r -> assertThat(r.getAmount()).isEqualByComparingTo("-123.45"))
                .satisfies(r -> assertThat(r.getBaseAmount()).isEqualByComparingTo("-3086.25"))
                .returns("CZK", AccImportRecordEntity::getBaseCurrencyCode)
                .satisfies(r -> assertThat(r.getPayloadHash()).hasSize(64))
                .satisfies(r -> assertThat(r.getRawPayload()).contains("\"id\"").contains("\"r1\""));
    }

    @Test
    void shouldFinishRunWithoutCursorAndRememberSuccessfulSync() {
        connector.setRecords(List.of(bookedRecord("r1", "10"), bookedRecord("r2", "20")));

        importService.sync(connectionId);

        assertThat(connection())
                .returns(NOW, AccConnectionEntity::getLastSuccessfulSyncAt)
                .satisfies(c -> assertThat(c.getSyncState()).isNullOrEmpty());
    }

    @Test
    void shouldNotCreateDuplicatesNorTouchUnchangedRecordsWhenSyncIsRepeated() {
        connector.setRecords(List.of(bookedRecord("r1", "10"), bookedRecord("r2", "20")));
        importService.sync(connectionId);
        markPosted("r1");
        var before = staged("r1");
        clock.advance(Duration.ofHours(6));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(2, 0, 0, 0));
        assertThat(importRecordRepository.findAll()).hasSize(2);
        assertThat(staged("r1"))
                .returns(ImportRecordStatus.POSTED, AccImportRecordEntity::getStatus)
                .returns(before.getVersion(), AccImportRecordEntity::getVersion)
                .returns(NOW, AccImportRecordEntity::getFirstSeenAt)
                .returns(NOW.plus(Duration.ofHours(6)), AccImportRecordEntity::getLastSeenAt);
    }

    @Test
    void shouldUpdateChangedRecordAndQueueItForPostingAgain() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        markPosted("r1");
        var hashBefore = staged("r1").getPayloadHash();
        connector.setRecords(List.of(bookedRecord("r1", "15")));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 1, 0));
        assertThat(staged("r1"))
                .returns(ImportRecordStatus.NEW, AccImportRecordEntity::getStatus)
                .satisfies(r -> assertThat(r.getAmount()).isEqualByComparingTo("15"))
                .satisfies(r -> assertThat(r.getPayloadHash()).isNotEqualTo(hashBefore));
    }

    @Test
    void shouldResumeFromSavedCursorWithoutDuplicatesWhenSyncFailsBetweenPages() {
        connector.setRecords(List.of(bookedRecord("r1", "10"), bookedRecord("r2", "20"), bookedRecord("r3", "30")));
        connector.setBeforeFetch(cursor -> {
            if ("2".equals(cursor)) {
                throw new ConnectorTransientException("Source unavailable", new IllegalStateException("reset"));
            }
        });

        assertThatThrownBy(() -> importService.sync(connectionId)).isInstanceOf(ConnectorTransientException.class);
        assertThat(importRecordRepository.findAll()).extracting(AccImportRecordEntity::getExternalId)
                .containsExactlyInAnyOrder("r1", "r2");
        assertThat(connection().getLastSuccessfulSyncAt()).isNull();

        connector.setBeforeFetch(cursor -> { });
        clock.advance(Duration.ofMinutes(5));
        var result = importService.sync(connectionId);

        assertThat(connector.requestedCursors()).containsExactly(null, "1", "2", "2");
        assertThat(result).isEqualTo(new ImportResult(1, 1, 0, 0));
        assertThat(importRecordRepository.findAll())
                .extracting(AccImportRecordEntity::getExternalId, AccImportRecordEntity::getStatus)
                .containsExactlyInAnyOrder(
                        tuple("r1", ImportRecordStatus.NEW),
                        tuple("r2", ImportRecordStatus.NEW),
                        tuple("r3", ImportRecordStatus.NEW));
    }

    @Test
    void shouldMarkRecordsMissingFromSourceWithinWindowAsDeleted() {
        connector.setRecords(List.of(bookedRecord("r1", "10"), bookedRecord("r2", "20")));
        importService.sync(connectionId);
        connector.setRecords(List.of(bookedRecord("r2", "20")));
        clock.advance(Duration.ofHours(6));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 0, 1));
        assertThat(staged("r1"))
                .returns(ImportRecordStatus.DELETED, AccImportRecordEntity::getStatus)
                .returns(ExternalRecordState.DELETED, AccImportRecordEntity::getSourceState);
        assertThat(staged("r2").getStatus()).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldKeepRecordsOutsideOfWindowWhenTheyAreNotFetched() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        connector.setRecords(List.of());
        clock.advance(Duration.ofDays(365));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(0, 0, 0, 0));
        assertThat(staged("r1").getStatus()).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldMarkRecordDeletedInSourceAsDeleted() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        connector.setRecords(List.of(withState(bookedRecord("r1", "10"), ExternalRecordState.DELETED)));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 1, 0));
        assertThat(staged("r1").getStatus()).isEqualTo(ImportRecordStatus.DELETED);
    }

    @Test
    void shouldRestoreDeletedRecordWhenItReappearsInSource() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        connector.setRecords(List.of());
        clock.advance(Duration.ofHours(6));
        importService.sync(connectionId);
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        clock.advance(Duration.ofHours(6));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 1, 0));
        assertThat(staged("r1").getStatus()).isEqualTo(ImportRecordStatus.NEW);
    }

    @Test
    void shouldRestoreRecordInConflictWhenItReappearsInSource() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        connector.setRecords(List.of());
        clock.advance(Duration.ofHours(6));
        importService.sync(connectionId);
        var inConflict = staged("r1");
        inConflict.setStatus(ImportRecordStatus.CONFLICT);
        importRecordRepository.save(inConflict);
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        clock.advance(Duration.ofHours(6));

        importService.sync(connectionId);

        assertThat(staged("r1"))
                .returns(ImportRecordStatus.NEW, AccImportRecordEntity::getStatus)
                .returns(ExternalRecordState.BOOKED, AccImportRecordEntity::getSourceState);
    }

    @Test
    void shouldFetchLastSyncWindowDaysWhenNoImportStartDateIsSet() {
        importService.sync(connectionId);

        assertThat(connector.requestedWindows())
                .containsExactly(RecordWindow.between(LocalDate.of(2025, 11, 3), LocalDate.of(2026, 2, 1)));
    }

    @Test
    void shouldStartFirstSyncAtImportStartDate() {
        setImportStartDate("2024-06-01");

        importService.sync(connectionId);

        assertThat(connector.requestedWindows())
                .containsExactly(RecordWindow.between(LocalDate.of(2024, 6, 1), LocalDate.of(2026, 2, 1)));
    }

    @Test
    void shouldFetchOnlySyncWindowDaysAfterFirstSync() {
        setImportStartDate("2024-06-01");
        importService.sync(connectionId);
        clock.advance(Duration.ofDays(1));

        importService.sync(connectionId);

        assertThat(connector.requestedWindows()).last()
                .isEqualTo(RecordWindow.between(LocalDate.of(2025, 11, 4), LocalDate.of(2026, 2, 2)));
    }

    @Test
    void shouldNotFetchRecordsBeforeImportStartDateWithinSyncWindow() {
        setImportStartDate("2026-01-20");
        importService.sync(connectionId);
        clock.advance(Duration.ofDays(1));

        importService.sync(connectionId);

        assertThat(connector.requestedWindows()).containsOnly(
                RecordWindow.between(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 1)),
                RecordWindow.between(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 2)));
    }

    @Test
    void shouldFetchOnlyTodayWhenImportStartDateIsInFuture() {
        setImportStartDate("2026-05-01");

        importService.sync(connectionId);

        assertThat(connector.requestedWindows())
                .containsExactly(RecordWindow.between(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1)));
    }

    @Test
    void shouldSkipSyncWhenSourceDataRevisionIsUnchanged() {
        connector.setDataRevision("rev-1");
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        clock.advance(Duration.ofHours(6));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(ImportResult.EMPTY);
        assertThat(connector.requestedCursors()).hasSize(1);
        assertThat(connection())
                .returns(NOW.plus(Duration.ofHours(6)), AccConnectionEntity::getLastSuccessfulSyncAt)
                .returns(Map.of("dataRevision", "rev-1"), AccConnectionEntity::getSyncState);
    }

    @Test
    void shouldSyncWhenSourceDataRevisionChanged() {
        connector.setDataRevision("rev-1");
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);
        connector.setDataRevision("rev-2");
        connector.setRecords(List.of(bookedRecord("r1", "15")));

        var result = importService.sync(connectionId);

        assertThat(result).isEqualTo(new ImportResult(1, 0, 1, 0));
        assertThat(connection().getSyncState()).isEqualTo(Map.of("dataRevision", "rev-2"));
    }

    @Test
    void shouldResumeUnfinishedRunWithItsDataRevisionWithoutAskingSourceAgain() {
        connector.setDataRevision("rev-1");
        connector.setRecords(List.of(bookedRecord("r1", "10"), bookedRecord("r2", "20")));
        connector.setBeforeFetch(cursor -> {
            if ("1".equals(cursor)) {
                throw new ConnectorTransientException("Source unavailable", new IllegalStateException("reset"));
            }
        });
        assertThatThrownBy(() -> importService.sync(connectionId)).isInstanceOf(ConnectorTransientException.class);
        connector.setBeforeFetch(cursor -> { });
        connector.setDataRevision("rev-2");

        importService.sync(connectionId);

        assertThat(connector.requestedCursors()).containsExactly(null, "1", "1");
        assertThat(connection().getSyncState()).isEqualTo(Map.of("dataRevision", "rev-1"));
    }

    @Test
    void shouldRejectSecondSyncOfSameConnectionWhileOneIsRunning() {
        var tenant = TenantContext.getCurrentTenant();
        var concurrentFailure = new AtomicReference<Throwable>();
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        connector.setBeforeFetch(cursor -> concurrentFailure.set(syncInOtherThread(tenant)));

        importService.sync(connectionId);

        assertThat(concurrentFailure.get())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
        assertThat(importRecordRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldRejectSyncOfDisabledConnection() {
        connectionService.setEnabled(connectionId, false);

        assertThatThrownBy(() -> importService.sync(connectionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldFailForMissingConnection() {
        assertThatThrownBy(() -> importService.sync(42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Connection not found");
    }

    @Test
    void shouldDeleteStagedRecordsWithTheirConnection() {
        connector.setRecords(List.of(bookedRecord("r1", "10")));
        importService.sync(connectionId);

        connectionService.deleteConnection(connectionId);

        assertThat(importRecordRepository.count()).isZero();
    }

    private Throwable syncInOtherThread(TenantIdentifier tenant) {
        try {
            CompletableFuture.runAsync(() -> {
                TenantContext.setCurrentTenant(tenant);
                try {
                    importService.sync(connectionId);
                } finally {
                    TenantContext.clear();
                }
            }).join();
            return null;
        } catch (CompletionException e) {
            return e.getCause();
        }
    }

    private static ExternalRecord withState(ExternalRecord source, ExternalRecordState state) {
        return ExternalRecord.builder()
                .externalId(source.externalId())
                .externalAccountId(source.externalAccountId())
                .date(source.date())
                .amount(source.amount())
                .state(state)
                .updatedAt(source.updatedAt())
                .rawPayload(source.rawPayload())
                .build();
    }

    private AccImportRecordEntity staged(String externalId) {
        return importRecordRepository.findByConnectionIdAndExternalId(connectionId, externalId).orElseThrow();
    }

    private void setImportStartDate(String date) {
        var connection = connection();
        connection.getSyncSettings().put(ImportService.IMPORT_START_DATE_SETTING, date);
        connectionRepository.save(connection);
    }

    private AccConnectionEntity connection() {
        return connectionRepository.findById(connectionId).orElseThrow();
    }

    private void markPosted(String externalId) {
        jdbcTemplate.update("UPDATE " + SCHEMA + ".acc_import_record SET status = 'POSTED' WHERE external_id = ?",
                externalId);
    }
}
