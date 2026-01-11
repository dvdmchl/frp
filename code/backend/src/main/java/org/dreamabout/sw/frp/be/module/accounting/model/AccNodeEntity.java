package org.dreamabout.sw.frp.be.module.accounting.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.dreamabout.sw.frp.be.module.common.domain.Node;

@Entity
@Getter
@Setter
@Table(name = "acc_node")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccNodeEntity implements Node {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "acc_node_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acc_node_id_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id",
            comment = "Reference to the parent node in the accounting hierarchy")
    private AccNodeEntity parent;

    @Column(name = "is_placeholder", nullable = false,
            comment = "Indicates if this node is a placeholder for organizational purposes")
    private Boolean isPlaceholder = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id",
            comment = "Reference to the account associated with this node")
    private AccAccountEntity account;

    @Column(name = "order_index", nullable = false,
            comment = "Index to determine the order of nodes under the same parent")
    private Integer orderIndex = 0;

    @Override
    public Boolean isPlaceholder() {
        return Boolean.TRUE.equals(isPlaceholder);
    }
}
