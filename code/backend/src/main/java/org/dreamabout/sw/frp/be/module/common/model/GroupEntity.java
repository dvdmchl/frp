package org.dreamabout.sw.frp.be.module.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.dreamabout.sw.frp.be.domain.Constant;

import uk.co.jemos.podam.common.PodamExclude;

import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "frp_group", schema = Constant.PUBLIC_SCHEMA)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GroupEntity extends AuditableEntity implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "frp_group_id_seq")
    @SequenceGenerator(name = "frp_group_id_seq", schema = Constant.PUBLIC_SCHEMA, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToMany(mappedBy = "groups")
    @PodamExclude
    private Set<UserEntity> users;
}
