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
@Constraint(validatedBy = BalancedTransactionValidator.class)
@Documented
public @interface BalancedTransaction {

    String message() default "Transaction is not balanced. Sum of credits must equal sum of debits.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
