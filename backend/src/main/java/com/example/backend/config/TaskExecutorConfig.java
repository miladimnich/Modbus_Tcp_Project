package com.example.backend.config;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskExecutorConfig {
  private static final Logger logger = LoggerFactory.getLogger(TaskExecutorConfig.class);

  @Bean
  public ScheduledExecutorService scheduledExecutorService() {
    int corePoolSize = Runtime.getRuntime().availableProcessors();
    System.out.println("max threads " + corePoolSize);
    return new ScheduledThreadPoolExecutor(corePoolSize, new ThreadPoolExecutor.AbortPolicy());
  }
}

