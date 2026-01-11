package org.dreamabout.sw.frp.be.module.accounting.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = JournalEntryValidator.class)
@Documented
public @interface ValidJournalEntry {

    String message() default "Journal entry must have either positive Credit or Debit, but not both.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
