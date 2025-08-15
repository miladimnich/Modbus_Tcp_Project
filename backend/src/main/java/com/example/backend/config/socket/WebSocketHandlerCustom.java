package com.example.backend.config.socket;

import com.example.backend.config.PollingState;

import com.example.backend.config.threading.ScheduledTaskRegistry;
import com.example.backend.events.StartPollingEvent;
import com.example.backend.events.StopPollingEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
@Getter
public class WebSocketHandlerCustom extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<WebSocketSession> connectedSessions = ConcurrentHashMap.newKeySet();
    private final Map<WebSocketSession, Integer> sessionTestStationMap = new ConcurrentHashMap<>(); // Track device per session
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService pollingExecutor;
    private final ScheduledTaskRegistry scheduledTaskRegistry;
    private final PollingState pollingState;


    private final Queue<Map<String, Object>> updateQueue = new ConcurrentLinkedQueue<>();
    private static final int MAX_BATCH_SIZE = 10;
    private static final int MAX_QUEUE_SIZE = 50;


    @Autowired
    public WebSocketHandlerCustom(ApplicationEventPublisher eventPublisher, ScheduledExecutorService pollingExecutor, ScheduledTaskRegistry scheduledTaskRegistry, PollingState pollingState) {
        this.eventPublisher = eventPublisher;
        this.pollingExecutor = pollingExecutor;
        this.scheduledTaskRegistry = scheduledTaskRegistry;
        this.pollingState = pollingState;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        if (!connectedSessions.contains(session)) {
            connectedSessions.add(session);
            log.info("WebSocket session established: {}", session.getId());
            log.info("Connected sessions count: {}", connectedSessions.size());
        } else {
            log.warn("Session {} is already present in connectedSessions.", session.getId());
        }
    }


    // grabs data from updateQueue and pushes to clients
    public void startWebSocketUpdateTask(int testStationId) {
        log.info("Starting WebSocket update task.");
        ScheduledFuture<?> task = pollingExecutor.scheduleWithFixedDelay(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("WebSocket update task interrupted. Exiting early.");
                    return; // exit task early
                }
                if (!updateQueue.isEmpty()) {
                    List<Map<String, Object>> batch = new ArrayList<>();
                    int count = 0;

                    while (!updateQueue.isEmpty() && count < MAX_BATCH_SIZE) { //10
                        Map<String, Object> entry = updateQueue.poll(); //It retrieves and removes the first (head) element from the queue.
                        if (entry != null) {
                            batch.add(entry);
                            count++;
                        } else {
                            log.warn("Null entry polled from updateQueue, skipping.");
                        }
                    }
                    log.debug("Pushing batch of {} updates to clients.", batch.size());

                    pushDataToClients(batch);
                } else {
                    log.debug("Update queue is empty; no data to push.");
                }
            } catch (Exception e) {
                log.error("Exception occurred during WebSocket update task execution", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
        scheduledTaskRegistry.register(testStationId, task);  // Add to list of scheduled tasks
        log.info("WebSocket update task scheduled with task id: {}", task);
    }

    public void enqueueUpdate(Map<String, Object> update) {
        while (updateQueue.size() >= MAX_QUEUE_SIZE) {
            log.warn("Queue is full, applying backpressure...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return; // Exit if interrupted
            }
        }
        updateQueue.add(update);
    }

    public void pushDataToClients(Object dataBatch) {
        if (dataBatch == null) {
            log.warn("pushDataToClients called with null dataBatch, skipping.");
            return;
        }
        Set<WebSocketSession> sessionsSnapshot = new HashSet<>(connectedSessions);
        for (WebSocketSession session : sessionsSnapshot) {
            if (session != null && session.isOpen()) {
                try {
                    log.debug("Attempting to serialize dataBatch for session {}: {}", session.getId(), dataBatch);
                    String messagePayload = objectMapper.writeValueAsString(dataBatch);
                    log.debug("Serialized payload for session {}: {}", session.getId(), messagePayload);
                    synchronized (session) {
                        session.sendMessage(new TextMessage(messagePayload));
                    }
                    log.debug("Sent message to session {}", session.getId());
                } catch (IllegalStateException | IOException e) {
                    log.warn("Error sending message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    // Manually flush the update queue to open sessions before shutting down
    public void flushPendingDataToOpenSessionsBeforeShutdown(int testStationId) {
        if (updateQueue.isEmpty() && connectedSessions.isEmpty()) {
            log.info("No pending data to flush or no active WebSocket sessions for device {}", testStationId);
            return;
        }

        log.info("Flushing updateQueue to WebSocket clients for testStationId {}", testStationId);
        log.debug("updateQueue contents: {}", new ArrayList<>(updateQueue));

        List<Map<String, Object>> batch = new ArrayList<>();
        Map<String, Object> entry;
        while ((entry = updateQueue.poll()) != null) {
            batch.add(entry);
        }
        if (!batch.isEmpty()) {
            System.out.println("getConnectedSessions " + getConnectedSessions().isEmpty());
            if (!getConnectedSessions().isEmpty()) {
                try {
                    pushDataToClients(batch);
                } catch (Exception e) {
                    log.error("Error while pushing data to clients for device {}: {}", testStationId, e.getMessage(), e);
                }
            } else {
                log.info("No open sessions to push data for device {}", testStationId);

            }

        } else {
            log.info("No data to push for testStationID {}", testStationId);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        log.info("afterConnectionClosed called for session {} at {}", session.getId(), Instant.now());
        Integer closedTestStationId = sessionTestStationMap.remove(session); // remove key
        connectedSessions.remove(session);
        System.out.println("currentStation from after connection closed " + pollingState.getCurrentTestStationId().get());
        log.info("closedTestStationId = {}", closedTestStationId);
        log.info("pollingState.getCurrentTestStationId() = {}", pollingState.getCurrentTestStationId().get());
        log.info("session.getId() = {}", session.getId());
        log.info("pollingState.getCurrentSessionId() = {}", pollingState.getCurrentSessionId().get());

        boolean isCurrentSession = session.getId().equals(pollingState.getCurrentSessionId().get());
        // Close session if open (optional)
        if (session.isOpen()) {
            try {
                log.info("Closing WebSocket session: {}", session.getId());
                session.close();
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session: {}", session.getId(), e);
            }
        } else {
            log.info("Session {} was already closed.", session.getId());
        }
        // most case when reset
        if (closedTestStationId != null && closedTestStationId.equals(pollingState.getCurrentTestStationId().get()) && isCurrentSession  && pollingState.getIsRunning().get()) {
            log.info("Closed session corresponds to active polling testStationId {}, stopping polling.", closedTestStationId + " active test station is " + pollingState.getCurrentTestStationId());
            eventPublisher.publishEvent(new StopPollingEvent(this, closedTestStationId));
            pollingState.getCurrentSessionId().set(null);
        } else {
            log.info("Skipping stopPolling — closed session's test station {} is not the active polling station {}.",
                    closedTestStationId, pollingState.getCurrentTestStationId().get());
        }
        // Always clear current session if it matches
        if (isCurrentSession) {
            pollingState.getCurrentSessionId().set(null);
        }
    }


    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        log.info("handleTextMessage called for session {} at {}", session.getId(), Instant.now());
        if (!session.isOpen()) {
            log.warn("Received message from closed session: {}", session.getId());
            return;
        }

        System.out.println("session id received from handleTextMessage " + session);
        String payload = message.getPayload();
        if (payload.isEmpty()) {
            log.warn("Received empty payload from session {}", session.getId());
            return;
        }

        Map<String, Object> messageData = objectMapper.readValue(payload, new TypeReference<>() {
        });
        String testStationIdString = (String) messageData.get("testStationId");
        log.info("Parsed testStationId: {} from session {}", testStationIdString, session.getId());
        if (testStationIdString == null || testStationIdString.equals("null")) {
            return;
        }

        try {
            int newTestStationId = Integer.parseInt(testStationIdString);

            System.out.println("CurrentSessionId() is " + pollingState.getCurrentSessionId().get());
            sessionTestStationMap.put(session, newTestStationId);
            pollingState.getCurrentSessionId().set(session.getId());
            log.info("pollingState.setCurrentTestStationId() = {}", pollingState.getCurrentTestStationId().get());
            log.info("Mapped session {} to testStationId {}", session.getId(), newTestStationId);
            log.info("Updated pollingState.getCurrentSessionId(){}", pollingState.getCurrentSessionId());

            eventPublisher.publishEvent(new StartPollingEvent(this, newTestStationId));

        } catch (NumberFormatException e) {
            log.warn("Invalid testStationId format: {} (payload: {})", testStationIdString, payload);
        }
    }
}