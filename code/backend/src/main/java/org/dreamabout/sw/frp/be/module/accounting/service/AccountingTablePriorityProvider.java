package org.dreamabout.sw.frp.be.module.accounting.service;

import org.dreamabout.sw.multitenancy.schema.TableCopyPriorityProvider;
import org.springframework.stereotype.Component;

@Component
public class AccountingTablePriorityProvider implements TableCopyPriorityProvider {

    @Override
    public Integer getTablePriority(String tableName) {
        return switch (tableName) {
            case "acc_currency" -> 1;
            case "acc_account" -> 2;
            case "acc_transaction" -> 3;
            case "acc_journal" -> 4;
            case "acc_node" -> 5;
            case "acc_connection" -> 6;
            case "acc_import_record" -> 7;
            case "acc_external_mapping" -> 8;
            default -> null;
        };
    }
}
