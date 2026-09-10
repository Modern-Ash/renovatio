package org.shark.renovatio.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Value("${renovatio.api.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${renovatio.api.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${renovatio.api.async.queue-capacity:100}")
    private int queueCapacity;

    @Bean("jobExecutor")
    public Executor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("job-");
        executor.initialize();
        return executor;
    }
}
