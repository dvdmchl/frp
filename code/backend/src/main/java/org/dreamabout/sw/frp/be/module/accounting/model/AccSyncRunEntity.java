package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncRunStatus;
import org.dreamabout.sw.frp.be.module.accounting.domain.SyncTrigger;
import org.dreamabout.sw.frp.be.module.common.model.IdAwareEntity;

import java.time.Instant;

/**
 * One synchronization of a connection with its counts. Scheduled runs have no user, so it carries no user audit fields.
 */
@Entity
@Getter
@Setter
@Table(name = "acc_sync_run")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccSyncRunEntity implements IdAwareEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_sync_run_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_sync_run_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "connection_id", nullable = false, updatable = false, comment = "Synchronized connection")
    private Long connectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, updatable = false, comment = "What started the run")
    private SyncTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, comment = "State of the run")
    private SyncRunStatus status;

    @Column(name = "started_at", nullable = false, updatable = false, comment = "Start of the run")
    private Instant startedAt;

    @Column(name = "finished_at", comment = "End of the run; NULL while running")
    private Instant finishedAt;

    @Column(name = "fetched_count", nullable = false, comment = "Records fetched from the source")
    private int fetched;

    @Column(name = "new_count", nullable = false, comment = "Records staged for the first time")
    private int created;

    @Column(name = "updated_count", nullable = false, comment = "Staged records changed in the source")
    private int updated;

    @Column(name = "deleted_count", nullable = false, comment = "Staged records deleted in the source")
    private int deleted;

    @Column(name = "posted_count", nullable = false, comment = "Records posted as transactions")
    private int posted;

    @Column(name = "error_count", nullable = false, comment = "Records that could not be posted (errors, conflicts)")
    private int errors;

    @Column(name = "error_message", comment = "Why the run failed")
    private String errorMessage;
}
