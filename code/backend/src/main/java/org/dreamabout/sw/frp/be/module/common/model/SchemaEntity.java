package org.dreamabout.sw.frp.be.module.common.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.domain.Constant;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "frp_schema", schema = Constant.PUBLIC_SCHEMA)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SchemaEntity extends AuditableEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "frp_schema_id_seq")
    @SequenceGenerator(name = "frp_schema_id_seq", allocationSize = 1, schema = Constant.PUBLIC_SCHEMA)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    @NotNull
    private Long ownerId;
}
