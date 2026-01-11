package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.accounting.validation.ValidJournalEntry;
import org.dreamabout.sw.frp.be.module.common.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "acc_journal")
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ValidJournalEntry
public class AccJournalEntity extends AuditableEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_journal_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_journal_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "date", nullable = false,
            comment = "Date of the journal entry")
    private LocalDate date;

    @Column(name = "description",
            comment = "Description of the journal entry")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false,
            comment = "Reference to the account associated with this journal entry")
    private AccAccountEntity account;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false,
            comment = "Reference to the transaction associated with this journal entry")
    private AccTransactionEntity transaction;

    @Column(name = "credit", nullable = false,
            comment = "Credit amount for the journal entry")
    @Min(0)
    private BigDecimal credit;

    @Column(name = "debit", nullable = false,
            comment = "Debit amount for the journal entry")
    @Min(0)
    private BigDecimal debit;
}
