package com.example.backend.config.threading;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.util.concurrent.*;

@Configuration
public class TaskExecutorConfig {

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        System.out.println("max threads " + corePoolSize);
        return new ScheduledThreadPoolExecutor(corePoolSize, new ThreadPoolExecutor.AbortPolicy());
    }
}
