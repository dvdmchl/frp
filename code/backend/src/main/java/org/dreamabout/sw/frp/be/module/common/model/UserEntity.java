package org.dreamabout.sw.frp.be.module.common.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.domain.Constant;
import org.dreamabout.sw.frp.be.model.AuditableEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "frp_user", schema = Constant.PUBLIC_SCHEMA)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class UserEntity extends AuditableEntity implements UserDetails {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "frp_user_id_seq")
    @SequenceGenerator(name = "frp_user_id_seq", schema = Constant.PUBLIC_SCHEMA, allocationSize = 1)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "last_login")
    private Instant lastLogin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schema_id")
    private SchemaEntity schema;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }
}
