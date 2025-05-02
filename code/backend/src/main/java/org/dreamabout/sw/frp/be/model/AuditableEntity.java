package org.dreamabout.sw.frp.be.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@MappedSuperclass
public abstract class AuditableEntity implements IdAwareEntity {

    @CreatedBy
    @Column(name = "created_by_user_id", updatable = false, nullable = false)
    private Long createdByUserId;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdDateTime;

    @LastModifiedBy
    @Column(name = "updated_by_user_id", nullable = false)
    private Long updatedByUserId;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedDateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
}
