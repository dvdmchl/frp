package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.model.AuditableEntity;

@Entity
@Getter
@Setter
@Table(name = "acc_journal")
public class AccJournalEntity extends AuditableEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_journal_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_journal_id_seq")
    private Long id;
}
