package com.sprintpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution
 * Used for background processing like work log synchronization
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Thread pool for work log synchronization
     * - Core pool: 5 threads (always available)
     * - Max pool: 10 threads (scales up when needed)
     * - Queue capacity: 100 tasks
     */
    @Bean(name = "workLogExecutor")
    public Executor workLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("WorkLog-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}

