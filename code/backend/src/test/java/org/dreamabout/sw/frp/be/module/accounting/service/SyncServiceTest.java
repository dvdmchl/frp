package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.config.db.TenantUtil;
import org.dreamabout.sw.frp.be.config.security.SecurityContextService;
import org.dreamabout.sw.frp.be.module.accounting.config.ConnectorProperties;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorAuthException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorNotReadyException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorRateLimitedException;
import org.dreamabout.sw.frp.be.module.accounting.connector.ConnectorTransientException;
import org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.accounting.model.AccConnectionEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccSyncRunEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccAccountCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionCreateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccConnectionFallbackRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.model.dto.AccExternalMappingUpdateRequestDto;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccConnectionRepository;
import org.dreamabout.sw.frp.be.module.accounting.repository.AccSyncRunRepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.dreamabout.sw.frp.be.module.accounting.connector.FakeAccountingConnector.bookedRecord;

// shares the application context (fake connector and clock) with ImportServiceTest
@Import(ImportServiceTest.ImportTestConfig.class)
class SyncServiceTest extends AbstractDbTest {

    private static final String SCHEMA = "sync_tenant";
    private static final String OTHER_SCHEMA = "sync_other_tenant";
    private static final String TYPE = "FAKE_IMPORT";
    private static final Instant NOW = Instant.parse("2026-02-01T08:00:00Z");
    private static final Duration INTERVAL = Duration.ofHours(6);

    @Autowired
    private SyncService syncService;
    @Autowired
    private SyncScheduler syncScheduler;
    @Autowired
    private ManualSyncService manualSyncService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private ExternalMappingService mappingService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccSyncRunRepository syncRunRepository;
    @Autowired
    private AccConnectionRepository connectionRepository;
    @Autowired
    private ConnectorProperties connectorProperties;
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

    private UserEntity owner;
    private Long connectionId;

    @BeforeEach
    void setUp() {
        connector.reset();
        clock.setInstant(NOW);
        owner = signIn("sync@test.com", SCHEMA);
        connectionId = createConnection("Wallet");
    }

    @AfterEach
    void clearTenant() {
        connectorProperties.setSyncRunsKept(50);
        TenantContext.clear();
        securityContextService.clearContext();
    }

    @Test
    void shouldRecordSuccessfulRunWithCounts() {
        prepareForPosting(connectionId);
        connector.setRecords(List.of(bookedRecord("r1", "-10"), bookedRecord("r2", "-20")));

        var run = syncService.sync(connectionId, SyncTrigger.SCHEDULED);

        assertThat(runs()).singleElement().satisfies(stored -> assertThat(stored)
                .returns(run.getId(), AccSyncRunEntity::getId)
                .returns(SyncTrigger.SCHEDULED, AccSyncRunEntity::getTrigger)
                .returns(SyncRunStatus.SUCCESS, AccSyncRunEntity::getStatus)
                .returns(NOW, AccSyncRunEntity::getStartedAt)
                .returns(NOW, AccSyncRunEntity::getFinishedAt)
                .returns(2, AccSyncRunEntity::getFetched)
                .returns(2, AccSyncRunEntity::getCreated)
                .returns(0, AccSyncRunEntity::getUpdated)
                .returns(0, AccSyncRunEntity::getDeleted)
                .returns(2, AccSyncRunEntity::getPosted)
                .returns(0, AccSyncRunEntity::getErrors)
                .returns(null, AccSyncRunEntity::getErrorMessage));
    }

    @Test
    void shouldScheduleNextSyncAfterIntervalOfConnection() {
        syncService.sync(connectionId, SyncTrigger.SCHEDULED);

        assertThat(connection().getNextSyncAt()).isEqualTo(NOW.plus(INTERVAL));
    }

    @Test
    void shouldRecordRunAsPartialWhenRecordCannotBePosted() {
        prepareForPosting(connectionId);
        connector.setRecords(List.of(bookedRecord("r1", "-10"), bookedRecord("r2", "0")));

        syncService.sync(connectionId, SyncTrigger.SCHEDULED);

        assertThat(runs()).singleElement()
                .returns(SyncRunStatus.PARTIAL, AccSyncRunEntity::getStatus)
                .returns(1, AccSyncRunEntity::getPosted)
                .returns(1, AccSyncRunEntity::getErrors);
    }

    @Test
    void shouldRecordFailedRunAndRethrowError() {
        failFetchWith(new ConnectorTransientException("Source unavailable", new IllegalStateException("reset")));

        assertThatThrownBy(() -> syncService.sync(connectionId, SyncTrigger.SCHEDULED))
                .isInstanceOf(ConnectorTransientException.class);

        assertThat(runs()).singleElement()
                .returns(SyncRunStatus.FAILED, AccSyncRunEntity::getStatus)
                .returns(NOW, AccSyncRunEntity::getFinishedAt)
                .returns("reset", AccSyncRunEntity::getErrorMessage);
        assertThat(connection().getNextSyncAt()).isEqualTo(NOW.plus(INTERVAL));
    }

    @Test
    void shouldRetryRateLimitedSourceAfterItsDelay() {
        failFetchWith(new ConnectorRateLimitedException("Too many requests", Duration.ofMinutes(10)));

        assertThatThrownBy(() -> syncService.sync(connectionId, SyncTrigger.SCHEDULED))
                .isInstanceOf(ConnectorRateLimitedException.class);

        assertThat(runs()).singleElement().returns("Too many requests", AccSyncRunEntity::getErrorMessage);
        assertThat(connection().getNextSyncAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void shouldRetrySourceThatIsNotReadyAfterConfiguredDelay() {
        failFetchWith(new ConnectorNotReadyException("Initial sync in progress"));

        assertThatThrownBy(() -> syncService.sync(connectionId, SyncTrigger.SCHEDULED))
                .isInstanceOf(ConnectorNotReadyException.class);

        assertThat(connection().getNextSyncAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    void shouldPauseScheduleWhenSourceRejectsCredentials() {
        failFetchWith(new ConnectorAuthException("Token expired"));

        assertThatThrownBy(() -> syncService.sync(connectionId, SyncTrigger.SCHEDULED))
                .isInstanceOf(ConnectorAuthException.class);
        clock.advance(INTERVAL.multipliedBy(2));

        assertThat(connection().getCredentialsRejected()).isTrue();
        assertThat(syncService.findDueForSync()).isEmpty();
    }

    @Test
    void shouldFindConnectionDueUntilItsFirstSyncAndAgainAfterInterval() {
        assertThat(syncService.findDueForSync()).containsExactly(connectionId);

        syncService.sync(connectionId, SyncTrigger.SCHEDULED);
        assertThat(syncService.findDueForSync()).isEmpty();

        clock.advance(INTERVAL);
        assertThat(syncService.findDueForSync()).containsExactly(connectionId);
    }

    @Test
    void shouldNotFindDisabledConnectionDue() {
        connectionService.setEnabled(connectionId, false);

        assertThat(syncService.findDueForSync()).isEmpty();
    }

    @Test
    void shouldFailRunLeftRunningByApplicationStop() {
        var leftover = new AccSyncRunEntity();
        leftover.setConnectionId(connectionId);
        leftover.setTrigger(SyncTrigger.SCHEDULED);
        leftover.setStatus(SyncRunStatus.RUNNING);
        leftover.setStartedAt(NOW.minus(INTERVAL));
        var leftoverId = syncRunRepository.save(leftover).getId();

        syncService.sync(connectionId, SyncTrigger.SCHEDULED);

        assertThat(syncRunRepository.findById(leftoverId).orElseThrow())
                .returns(SyncRunStatus.FAILED, AccSyncRunEntity::getStatus)
                .returns(NOW, AccSyncRunEntity::getFinishedAt)
                .returns(SyncRunRecorder.INTERRUPTED, AccSyncRunEntity::getErrorMessage);
    }

    @Test
    void shouldKeepOnlyConfiguredNumberOfLatestRuns() {
        connectorProperties.setSyncRunsKept(2);
        for (int i = 0; i < 3; i++) {
            syncService.sync(connectionId, SyncTrigger.SCHEDULED);
            clock.advance(INTERVAL);
        }

        assertThat(runs()).extracting(AccSyncRunEntity::getStartedAt)
                .containsExactly(NOW.plus(INTERVAL.multipliedBy(2)), NOW.plus(INTERVAL));
    }

    @Test
    void shouldNotRecordRunWhenConnectionIsSynchronizedAlready() {
        var tenant = TenantContext.getCurrentTenant();
        var concurrentFailure = new AtomicReference<Throwable>();
        connector.setBeforeFetch(cursor -> concurrentFailure.set(syncInOtherThread(tenant)));

        syncService.sync(connectionId, SyncTrigger.SCHEDULED);

        assertThat(concurrentFailure.get()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
        assertThat(runs()).hasSize(1);
    }

    @Test
    void shouldSyncDueConnectionsOfAllTenants() {
        signIn("other@test.com", OTHER_SCHEMA);
        var otherTenantConnectionId = createConnection("Bank");
        signIn(owner);

        runScheduler();

        assertThat(runs()).singleElement().returns(SyncTrigger.SCHEDULED, AccSyncRunEntity::getTrigger);
        assertThat(inTenant(OTHER_SCHEMA, () -> syncRunRepository
                .findByConnectionIdOrderByStartedAtDescIdDesc(otherTenantConnectionId))).hasSize(1);
    }

    @Test
    void shouldNotSyncConnectionWhoseIntervalHasNotElapsed() {
        syncService.sync(connectionId, SyncTrigger.SCHEDULED);
        clock.advance(INTERVAL.minusMinutes(1));

        runScheduler();

        assertThat(runs()).hasSize(1);
    }

    @Test
    void shouldGoOnWithOtherConnectionsWhenScheduledSyncFails() {
        var secondConnectionId = createConnection("Second wallet");
        var fetches = new AtomicInteger();
        connector.setBeforeFetch(cursor -> {
            if (fetches.getAndIncrement() == 0) {
                throw new ConnectorNotReadyException("Initial sync in progress");
            }
        });

        runScheduler();

        assertThat(runs()).singleElement().returns(SyncRunStatus.FAILED, AccSyncRunEntity::getStatus);
        assertThat(syncRunRepository.findByConnectionIdOrderByStartedAtDescIdDesc(secondConnectionId)).singleElement()
                .returns(SyncRunStatus.SUCCESS, AccSyncRunEntity::getStatus);
    }

    @Test
    void shouldSyncNowInBackgroundInTenantAndAsUserOfCaller() {
        prepareForPosting(connectionId);
        connector.setRecords(List.of(bookedRecord("r1", "-10")));

        var run = manualSyncService.syncNow(connectionId).join();

        assertThat(run).returns(SyncTrigger.MANUAL, AccSyncRunEntity::getTrigger)
                .returns(SyncRunStatus.SUCCESS, AccSyncRunEntity::getStatus);
        assertThat(runs()).singleElement().returns(run.getId(), AccSyncRunEntity::getId);
        assertThat(jdbcTemplate.queryForObject("SELECT created_by_user_id FROM " + SCHEMA + ".acc_transaction",
                Long.class)).isEqualTo(owner.getId());
        assertThat(TenantContext.getCurrentTenant().getTenantId()).isEqualTo(SCHEMA);
    }

    @Test
    void shouldRejectSyncNowOfDisabledConnectionRightAway() {
        connectionService.setEnabled(connectionId, false);

        assertThatThrownBy(() -> manualSyncService.syncNow(connectionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
        assertThat(runs()).isEmpty();
    }

    private UserEntity signIn(String email, String schema) {
        var user = new UserEntity();
        user.setEmail(email);
        user.setPassword("password");
        var saved = userRepository.save(user);
        saved.setSchema(schemaService.createSchema(schema, saved.getId()));
        saved = userRepository.save(saved);
        signIn(saved);
        return saved;
    }

    private void signIn(UserEntity user) {
        securityContextService.setAuthentication(new TestingAuthenticationToken(user, null, List.of()));
        TenantContext.setCurrentTenant(tenantUtil.getCurrentTenantIdentifier());
    }

    private <T> T inTenant(String schema, Supplier<T> action) {
        var current = TenantContext.getCurrentTenant();
        TenantContext.setCurrentTenant(TenantIdentifier.of(schema));
        try {
            return action.get();
        } finally {
            TenantContext.setCurrentTenant(current);
        }
    }

    /**
     * Runs the scheduler like its own thread would and restores the tenant of the test, which the scheduler clears.
     */
    private void runScheduler() {
        syncScheduler.syncDueConnections();
        signIn(owner);
    }

    private Long createConnection(String name) {
        return connectionService.createConnection(new AccConnectionCreateRequestDto(TYPE, name,
                Map.of(FakeAccountingConnector.TOKEN, FakeAccountingConnector.VALID_TOKEN), Map.of())).id();
    }

    /**
     * Maps the external account of {@link FakeAccountingConnector#bookedRecord} and sets a fallback expense account,
     * so its records can be posted.
     */
    private void prepareForPosting(Long id) {
        var cash = accountService.createAccount(new AccAccountCreateRequestDto(null, "Cash", null, "CZK", true,
                AccAcountType.ASSET, false)).account().id();
        var expense = accountService.createAccount(new AccAccountCreateRequestDto(null, "Expenses", null, "CZK", false,
                AccAcountType.EXPENSE, false)).account().id();
        var mappingId = mappingService.refreshMappings(id).stream()
                .filter(mapping -> mapping.externalId().equals("acc-1"))
                .findFirst().orElseThrow().id();
        mappingService.updateMapping(mappingId, new AccExternalMappingUpdateRequestDto(cash, false));
        connectionService.setFallbackAccounts(id, new AccConnectionFallbackRequestDto(expense, null));
    }

    private void failFetchWith(RuntimeException failure) {
        connector.setRecords(List.of(bookedRecord("r1", "-10")));
        connector.setBeforeFetch(cursor -> {
            throw failure;
        });
    }

    private Throwable syncInOtherThread(TenantIdentifier tenant) {
        try {
            CompletableFuture.runAsync(() -> {
                TenantContext.setCurrentTenant(tenant);
                try {
                    syncService.sync(connectionId, SyncTrigger.MANUAL);
                } finally {
                    TenantContext.clear();
                }
            }).join();
            return null;
        } catch (CompletionException e) {
            return e.getCause();
        }
    }

    private List<AccSyncRunEntity> runs() {
        return syncRunRepository.findByConnectionIdOrderByStartedAtDescIdDesc(connectionId);
    }

    private AccConnectionEntity connection() {
        return connectionRepository.findById(connectionId).orElseThrow();
    }
}
