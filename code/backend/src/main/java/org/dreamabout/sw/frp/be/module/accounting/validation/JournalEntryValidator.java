package org.dreamabout.sw.frp.be.module.accounting.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;

import java.math.BigDecimal;

public class JournalEntryValidator implements ConstraintValidator<ValidJournalEntry, AccJournalEntity> {

    @Override
    public boolean isValid(AccJournalEntity journal, ConstraintValidatorContext context) {
        if (journal == null) {
            return true;
        }

        BigDecimal credit = journal.getCredit() != null ? journal.getCredit() : BigDecimal.ZERO;
        BigDecimal debit = journal.getDebit() != null ? journal.getDebit() : BigDecimal.ZERO;

        boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;
        boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;

        // XOR: Must have one positive, but not both
        return hasCredit ^ hasDebit;
    }
}
