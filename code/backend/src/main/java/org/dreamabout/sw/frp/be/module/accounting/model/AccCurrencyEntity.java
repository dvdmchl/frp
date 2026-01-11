package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.common.model.AuditableEntity;

@Entity
@Getter
@Setter
@Table(name = "acc_currency")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class AccCurrencyEntity extends AuditableEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_currency_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_currency_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "code", nullable = false, unique = true,
            comment = "ISO 4217 currency code, e.g., 'USD', 'EUR'")
    @Size(min = 3, max = 3)
    private String code;

    @Column(name = "name", nullable = false,
            comment = "Full name of the currency, e.g., 'United States Dollar'")
    private String name;

    @Column(name = "is_base", nullable = false,
            comment = "Indicates if this currency is the base currency for the accounting system")
    private Boolean isBase = false;

    @Column(name = "scale", nullable = false,
            comment = "Number of decimal places used for this currency")
    @Min(0)
    private Integer scale;
}
