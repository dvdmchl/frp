package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.frp.be.module.common.service.TablePriorityProvider;
import org.springframework.stereotype.Component;

@Component
public class AccountingTablePriorityProvider implements TablePriorityProvider {

    @Override
    public Integer getTablePriority(String tableName) {
        return switch (tableName) {
            case "acc_currency" -> 1;
            case "acc_account" -> 2;
            case "acc_transaction" -> 3;
            case "acc_journal" -> 4;
            case "acc_node" -> 5;
            default -> null;
        };
    }
}
