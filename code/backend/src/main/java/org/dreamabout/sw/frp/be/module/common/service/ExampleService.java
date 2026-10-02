package org.dreamabout.sw.frp.be.module.common.service;

import org.dreamabout.sw.multitenancy.core.MultitenancyThreadContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    @Async
    public void asyncProcess(String value) {
        MultitenancyThreadContext.set("asyncValue", value);
        // logic continues no need to clear context
    }

    @Scheduled(initialDelay = 1000, fixedDelay = Long.MAX_VALUE)
    public void scheduledJob() {
        MultitenancyThreadContext.set("schedValue", "run");
        // logic continues no need to clear context
    }
}
