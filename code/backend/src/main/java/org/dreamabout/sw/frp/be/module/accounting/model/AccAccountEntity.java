package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.accounting.domain.AccAcountType;
import org.dreamabout.sw.frp.be.module.common.model.AuditableEntity;

@Entity
@Getter
@Setter
@Table(name = "acc_account")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AccAccountEntity extends AuditableEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_account_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_account_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true,
            comment = "Unique name of the account")
    private String name;

    @Column(name = "description",
            comment = "Detailed description of the account")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id",
            comment = "Reference to the currency used for this account")
    private AccCurrencyEntity currency;

    @Column(name = "is_liquid", nullable = false
            , comment = "Indicates if the account is a liquid asset")
    private Boolean isLiquid;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type",
            comment = "Type of the account: ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE; empty for placeholders")
    private AccAcountType accountType;
}
