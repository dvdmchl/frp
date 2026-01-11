package org.dreamabout.sw.frp.be.module.accounting.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.dreamabout.sw.frp.be.module.accounting.model.AccJournalEntity;
import org.dreamabout.sw.frp.be.module.accounting.model.AccTransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingValidatorsTest {

    private JournalEntryValidator journalEntryValidator;
    private BalancedTransactionValidator balancedTransactionValidator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        journalEntryValidator = new JournalEntryValidator();
        balancedTransactionValidator = new BalancedTransactionValidator();
    }

    @Test
    void journalEntry_valid_credit() {
        var j = new AccJournalEntity();
        j.setCredit(BigDecimal.TEN);
        j.setDebit(BigDecimal.ZERO);
        assertThat(journalEntryValidator.isValid(j, context)).isTrue();
    }

    @Test
    void journalEntry_valid_debit() {
        var j = new AccJournalEntity();
        j.setCredit(BigDecimal.ZERO);
        j.setDebit(BigDecimal.TEN);
        assertThat(journalEntryValidator.isValid(j, context)).isTrue();
    }

    @Test
    void journalEntry_invalid_both() {
        var j = new AccJournalEntity();
        j.setCredit(BigDecimal.ONE);
        j.setDebit(BigDecimal.ONE);
        assertThat(journalEntryValidator.isValid(j, context)).isFalse();
    }

    @Test
    void journalEntry_invalid_none() {
        var j = new AccJournalEntity();
        j.setCredit(BigDecimal.ZERO);
        j.setDebit(BigDecimal.ZERO);
        assertThat(journalEntryValidator.isValid(j, context)).isFalse();
    }

    @Test
    void transaction_valid_balanced() {
        var t = new AccTransactionEntity();
        var journals = new ArrayList<AccJournalEntity>();

        var j1 = new AccJournalEntity();
        j1.setCredit(BigDecimal.TEN);
        j1.setDebit(BigDecimal.ZERO);

        var j2 = new AccJournalEntity();
        j2.setCredit(BigDecimal.ZERO);
        j2.setDebit(BigDecimal.TEN);

        journals.add(j1);
        journals.add(j2);
        t.setJournals(journals);

        assertThat(balancedTransactionValidator.isValid(t, context)).isTrue();
    }

    @Test
    void transaction_invalid_unbalanced() {
        var t = new AccTransactionEntity();
        var journals = new ArrayList<AccJournalEntity>();

        var j1 = new AccJournalEntity();
        j1.setCredit(BigDecimal.TEN);
        j1.setDebit(BigDecimal.ZERO);

        var j2 = new AccJournalEntity();
        j2.setCredit(BigDecimal.ZERO);
        j2.setDebit(BigDecimal.ONE);

        journals.add(j1);
        journals.add(j2);
        t.setJournals(journals);

        assertThat(balancedTransactionValidator.isValid(t, context)).isFalse();
    }
}
