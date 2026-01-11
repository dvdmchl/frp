package org.dreamabout.sw.frp.be.module.accounting.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccTransactionEntity;

import java.math.BigDecimal;

public class BalancedTransactionValidator implements ConstraintValidator<BalancedTransaction, AccTransactionEntity> {

    @Override
    public boolean isValid(AccTransactionEntity transaction, ConstraintValidatorContext context) {
        if (transaction == null || transaction.getJournals() == null || transaction.getJournals().isEmpty()) {
            // @NotEmpty on the list should handle the empty case
            return true;
        }

        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalDebit = BigDecimal.ZERO;

        for (AccJournalEntity journal : transaction.getJournals()) {
            totalCredit = totalCredit.add(journal.getCredit() != null ? journal.getCredit() : BigDecimal.ZERO);
            totalDebit = totalDebit.add(journal.getDebit() != null ? journal.getDebit() : BigDecimal.ZERO);
        }

        return totalCredit.compareTo(totalDebit) == 0;
    }
}
