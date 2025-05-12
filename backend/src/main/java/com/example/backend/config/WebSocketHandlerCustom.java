package com.example.backend.config;

import com.example.backend.events.StartPollingEvent;
import com.example.backend.events.StopPollingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WebSocketHandlerCustom extends TextWebSocketHandler {
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Getter
  private final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
  private final Map<WebSocketSession, Integer> sessionDeviceMap = new ConcurrentHashMap<>(); // Track device per session
  private final ApplicationEventPublisher eventPublisher;
  private final ScheduledExecutorService pollingExecutor;

  private final Queue<Map<String, Object>> updateQueue = new ConcurrentLinkedQueue<>();
  private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();  // Track scheduled tasks
  private static final int MAX_BATCH_SIZE = 10;
  private static final int MAX_QUEUE_SIZE = 50;
  private int currentDeviceId = -1;


  @Autowired
  public WebSocketHandlerCustom(ApplicationEventPublisher eventPublisher, ScheduledExecutorService pollingExecutor) {
    this.eventPublisher = eventPublisher;
    this.pollingExecutor = pollingExecutor;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    if (!connectedSessions.contains(session)) {
      connectedSessions.add(session);
      System.out.println("WebSocket session established: " + session.getId());
      System.out.println("Connected sessions count: " + connectedSessions.size());
    }
  }

  public void startWebSocketUpdateTask() {
    ScheduledFuture<?> task = pollingExecutor.scheduleWithFixedDelay(() -> {
      if (!updateQueue.isEmpty()) {
        List<Map<String, Object>> batch = new ArrayList<>();
        int count = 0;

        while (!updateQueue.isEmpty() && count < MAX_BATCH_SIZE) {
          batch.add(updateQueue.poll());
          count++;
        }
        pushDataToClients(batch);
      }
    }, 0, 1, TimeUnit.SECONDS);

    scheduledTasks.add(task);  // Add to list of scheduled tasks
  }

  public void enqueueUpdate(Map<String, Object> update) {
    if (updateQueue.size() < MAX_QUEUE_SIZE) {
      updateQueue.add(update);  // Add update if the queue size is within limit
    } else {
      // Apply backpressure by waiting before adding more updates
      System.out.println("Queue is full, applying backpressure...");
      try {
        // Wait for a short time before retrying to add an update
        Thread.sleep(100);  // Adjust sleep time as necessary
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      enqueueUpdate(update);  // Retry adding the update after the delay
    }
  }

  public void pushDataToClients(Object dataBatch) {
    if (dataBatch == null) return;
    try {

      for (WebSocketSession session : connectedSessions) {
        if (session != null && session.isOpen()) {
          try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dataBatch)));

          } catch (Exception e) {
            System.out.println("Error sending message to session " + session.getId() + ": " + e.getMessage());
          }
        } else {
          System.out.println("Skipping closed WebSocket session: " + session.getId());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // Gracefully cancel tasks
  public void cancelScheduledTasks() {
    for (ScheduledFuture<?> task : scheduledTasks) {
      if (task != null && !task.isCancelled()) {
        task.cancel(false);  // Don't interrupt ongoing tasks, let them finish
      }
    }
    scheduledTasks.clear();  // Clear the list of tasks
  }

  // Manually flush the update queue to open sessions before shutting down
  public void flushPendingDataToOpenSessionsBeforeShutdown(int deviceId) {

    if (!updateQueue.isEmpty() && !connectedSessions.isEmpty()) {
      System.out.println("Flushing updateQueue to clients. Contents:");
      updateQueue.forEach(System.out::println);  // Log each item

      List<Map<String, Object>> batch = new ArrayList<>();
      while (!updateQueue.isEmpty()) {
        batch.add(updateQueue.poll());
      }

      pushDataToClients(batch);  // Push the remaining data to open sessions

      String response = "Polling stopped";
      Map<String, Object> stopMeasurement = new HashMap<>();


      try {
        System.out.println("Sending STOP_POLLING event to WebSocket clients...");
        stopMeasurement.put("stopPolling", response);
        stopMeasurement.put("deviceId", deviceId);
        pushDataToClients(stopMeasurement);

      } catch (Exception e) {
        System.err.println("Error while sending STOP_POLLING event: " + e.getMessage());
      }
    } else {
      System.out.println("Nothing to flush. updateQueue or connectedSessions is empty.");
    }
  }


  public void closeAllWebSocketSessions(int deviceId) {
    try {
      System.out.println("Preparing to close all WebSocket sessions...");
      System.out.println("updateQueue before flush:");
      updateQueue.forEach(System.out::println);


      flushPendingDataToOpenSessionsBeforeShutdown(deviceId);

      for (WebSocketSession session : connectedSessions) {
        if (session.isOpen()) {
          session.close();  // Close the WebSocket session
          System.out.println("WebSocket session closed.");
        }
      }

      System.out.println("updateQueue after flush and before clearing:");
      updateQueue.forEach(System.out::println);
      // Clear the sessions list after closing
      connectedSessions.clear();
      cancelScheduledTasks();  // Cancel all scheduled tasks
      System.out.println("updateQueue contents before clearing: " + new ArrayList<>(updateQueue));
      updateQueue.clear();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    Map<String, Object> messageData = objectMapper.readValue(message.getPayload(), Map.class);
    String deviceIdString = (String) messageData.get("deviceId"); // It will return "1" as a String
    if (deviceIdString == null || deviceIdString.equals("null")) {
      return;
    }
    try {
      int deviceId = Integer.parseInt(deviceIdString); // Safely parse the String to int
      sessionDeviceMap.put(session, deviceId);
      eventPublisher.publishEvent(new StartPollingEvent(this, deviceId));
    } catch (NumberFormatException e) {
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

    connectedSessions.remove(session);
    System.out.println("WebSocket session closed: " + session.getId());
    cancelScheduledTasks();  // Cancel all scheduled tasks
    Integer deviceId = sessionDeviceMap.remove(session);

    if (deviceId != null && connectedSessions.isEmpty()) {
      System.out.println("Stopping polling for device: " + deviceId);
      eventPublisher.publishEvent(new StopPollingEvent(this, deviceId));
      System.out.println("updateQueue contents before clearing: " + new ArrayList<>(updateQueue));
      updateQueue.clear();
    }
  }


  // Shutdown the executor gracefully
  @PreDestroy
  public void shutdown() {
    pollingExecutor.shutdown(); // Gracefully shut down the executor service
    try {
      if (!pollingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
        pollingExecutor.shutdownNow(); // Force shutdown if tasks don't finish in time
      }
    } catch (InterruptedException e) {
      pollingExecutor.shutdownNow(); // Force shutdown on interrupt
      Thread.currentThread().interrupt();
    }
  }
}