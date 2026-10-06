package org.dreamabout.sw.frp.be.module.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.module.common.domain.AuditAction;

import java.time.Instant;

/**
 * One record of the global audit log. Records are only appended, never updated.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "frp_audit_log", schema = Constant.PUBLIC_SCHEMA)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AuditLogEntity implements IdAwareEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "frp_audit_log_id_seq")
    @SequenceGenerator(name = "frp_audit_log_id_seq", schema = Constant.PUBLIC_SCHEMA, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "user_id", updatable = false)
    private Long userId;

    @Column(name = "user_email", updatable = false)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false)
    private AuditAction action;

    @Column(name = "resource", updatable = false)
    private String resource;

    @Column(name = "details", updatable = false)
    private String details;
}
