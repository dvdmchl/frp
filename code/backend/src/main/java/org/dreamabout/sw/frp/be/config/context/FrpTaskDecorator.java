package org.dreamabout.sw.frp.be.config.context;

import org.springframework.core.task.TaskDecorator;

/**
 * Decorator for tasks that need to be executed in a specific thread context.
 */
public class FrpTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            try {
                FrpThreadContext.init();
                runnable.run();
            } finally {
                FrpThreadContext.clear();
            }
        };
    }
}
