package org.dreamabout.sw.frp.be.module.common.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "frp_user", schema = Constant.PUBLIC_SCHEMA)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserEntity extends AuditableEntity implements UserDetails {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "frp_user_id_seq")
    @SequenceGenerator(name = "frp_user_id_seq", schema = Constant.PUBLIC_SCHEMA, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "full_name", unique = true)
    private String fullName;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "last_login")
    private Instant lastLogin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schema_id")
    private SchemaEntity schema;

    @Column(name = "token_valid", nullable = false)
    @Builder.Default
    private Boolean tokenValid = true;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "admin", nullable = false)
    @Builder.Default
    private Boolean admin = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "frp_user_group",
            schema = Constant.PUBLIC_SCHEMA,
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private Set<GroupEntity> groups = new java.util.HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (Boolean.TRUE.equals(admin)) {
            return List.of((GrantedAuthority) () -> "ROLE_ADMIN");
        }
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }
}
