package org.dreamabout.sw.frp.be.config.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class ThreadContextConfiguration implements AsyncConfigurer, SchedulingConfigurer {

    @Value("${frp.executor.corePoolSize:5}")
    private int corePoolSize;
    @Value("${frp.executor.maxPoolSize:5}")
    private int maxPoolSize;
    @Value("${frp.executor.queueCapacity:50}")
    private int queueCapacity;

    @Value("${frp.scheduler.poolSize:5}")
    private int schedulerPoolSize;

    @Bean("frpExecutor")
    public ThreadPoolTaskExecutor taskExecutor(TaskDecorator decorator) {
        ThreadPoolTaskExecutor tx = new ThreadPoolTaskExecutor();
        tx.setCorePoolSize(corePoolSize);
        tx.setMaxPoolSize(maxPoolSize);
        tx.setQueueCapacity(queueCapacity);
        tx.setTaskDecorator(decorator);
        tx.initialize();
        return tx;
    }

    @Bean("frpScheduler")
    public ThreadPoolTaskScheduler taskScheduler(TaskDecorator decorator) {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(schedulerPoolSize);
        ts.setTaskDecorator(decorator);
        ts.initialize();
        return ts;
    }

    @Bean
    public TaskDecorator taskDecorator() {
        return new FrpTaskDecorator();
    }

    // Pro @Async
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor(taskDecorator());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(ex.getMessage(), ex);
    }

    // Pro @Scheduled
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setScheduler(taskScheduler(taskDecorator()));
    }
}

