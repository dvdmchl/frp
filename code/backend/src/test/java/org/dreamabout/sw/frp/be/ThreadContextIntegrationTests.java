package org.dreamabout.sw.frp.be;

import org.dreamabout.sw.frp.be.config.context.FrpThreadContext;
import org.dreamabout.sw.frp.be.module.common.service.ExampleService;
import org.dreamabout.sw.frp.be.test.AbstractDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@SpringBootTest
@AutoConfigureMockMvc
class ThreadContextIntegrationTests extends AbstractDbTest {

    @Autowired
    private TaskDecorator decorator;

    @Autowired
    private ThreadPoolTaskScheduler scheduler;

    @Autowired
    private ExampleService exampleService;

    @Autowired
    private MockMvc mvc;

    @Test
    void executorDecorator_clearsAfterRun() {
        Runnable decorated = decorator.decorate(() -> {
            FrpThreadContext.set("key", "value");
            var storedValue = FrpThreadContext.get("key");
            assertThat(storedValue).isEqualTo("value");
        });

        decorated.run();

        var storedValue = FrpThreadContext.get("key");
        assertThat(storedValue).isNull();
    }

    @Test
    void scheduledTask_clearedAfterRun() throws Exception {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        scheduler.schedule(() -> {
            FrpThreadContext.set("sched", "ok");
            cf.complete(null);
        }, Instant.now());
        cf.get(1, TimeUnit.SECONDS);
        var storedValue = FrpThreadContext.get("sched");
        assertThat(storedValue).isNull();
    }

    @Test
    void asyncService_contextIsolatedAndCleared() {
        exampleService.asyncProcess("abc");
        await().atMost(500, TimeUnit.MILLISECONDS).until(() -> FrpThreadContext.get("asyncValue") == null);
        var storedValue = FrpThreadContext.get("asyncValue");
        assertThat(storedValue).isNull();
    }

    @Test
    @WithMockUser(username = "testuser")
    void httpFilter_setsAndClearsContext() throws Exception {
        mvc.perform(get("/api/context").header("X-Frp-Context", "test123"))
                .andExpect(status().isOk())
                .andExpect(content().string("test123"));
        var storedValue = FrpThreadContext.get("frpHeader");
        assertThat(storedValue).isNull();
    }
}
