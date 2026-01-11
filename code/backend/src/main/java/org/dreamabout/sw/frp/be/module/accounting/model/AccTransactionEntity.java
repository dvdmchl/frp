package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.accounting.validation.BalancedTransaction;
import org.dreamabout.sw.frp.be.module.common.model.AuditableEntity;

import java.math.BigDecimal;
import java.util.List;


@Entity
@Getter
@Setter
@Table(name = "acc_transaction")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@BalancedTransaction
public class AccTransactionEntity extends AuditableEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_transaction_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_transaction_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "reference",
            comment = "Reference code or number for the transaction")
    private String reference;

    @Column(name = "description",
            comment = "Detailed description of the transaction")
    private String description;

    @Column(name = "fx_rate",
            comment = "Foreign exchange rate applied to the transaction, if applicable")
    private BigDecimal fxRate;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotEmpty
    private List<AccJournalEntity> journals;
}
