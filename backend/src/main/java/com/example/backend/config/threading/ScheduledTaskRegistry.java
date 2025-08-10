package com.example.backend.config.threading;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class ScheduledTaskRegistry {
    private final ScheduledExecutorService scheduledExecutorService;
    private final Map<Integer, List<ScheduledFuture<?>>> taskMap = new ConcurrentHashMap<>();


    public ScheduledTaskRegistry(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }


    public void register(int testStationId, ScheduledFuture<?> task) {
        taskMap.computeIfAbsent(testStationId, k -> new ArrayList<>()).add(task);
        log.info("Registered new scheduled task [{}] for testStationId {}. Total tasks for this station: {}",
                task.toString(), testStationId, taskMap.get(testStationId).size());
    }




    public void cancelAll(int testStationId) {
        List<ScheduledFuture<?>> tasks = taskMap.remove(testStationId);
        if (tasks != null && !tasks.isEmpty()) {
            for (ScheduledFuture<?> task : tasks) {
                System.out.println("Cancelling task " + task.toString() + " for test station " + testStationId);
                task.cancel(false);
            }
            log.info("Cancelled {} scheduled task(s) for testStationId {}", tasks.size(), testStationId);
        } else {
            log.info("ℹ️ No scheduled tasks to cancel for testStationId {}", testStationId);
        }
    }



    public void cancelAll() {
        for (Integer testStationId : new ArrayList<>(taskMap.keySet())) {
            cancelAll(testStationId);
        }
    }

    @PreDestroy
    public void onShutdown() {
        System.out.println("Cancelling all registered scheduled tasks...");
        cancelAll(); // cancel before executor is shut down
        scheduledExecutorService.shutdown();
    }
}
