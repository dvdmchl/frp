package org.dreamabout.sw.frp.be.module.accounting.service;

import org.springframework.core.NestedExceptionUtils;

import java.util.Objects;

/**
 * Error messages stored for the user, e.g. with a failed record or run.
 */
final class ExceptionMessages {

    private ExceptionMessages() {
    }

    /**
     * Message of the most specific cause, or its class name when it has none.
     */
    static String of(Throwable e) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
        return Objects.requireNonNullElse(cause.getMessage(), cause.getClass().getSimpleName());
    }
}
