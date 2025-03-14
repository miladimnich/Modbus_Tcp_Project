package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
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



  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    if (!connectedSessions.contains(session)) {
      connectedSessions.add(session);
      System.out.println("WebSocket session established: " + session.getId());
      System.out.println("Connected sessions count: " + connectedSessions.size());

    }
  }


  public void pushDataToClients(Map<String, Object> data) {
    try {
      if (data == null || data.isEmpty()) {
        System.out.println("No data to send.");
        return;
      }
      for (WebSocketSession session : connectedSessions) {
        if (session.isOpen()) {
          session.sendMessage(new TextMessage(objectMapper.writeValueAsString(data)));

        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Close all active WebSocket sessions
  public void closeAllWebSocketSessions() {
    try {
      for (WebSocketSession session : connectedSessions) {
        if (session.isOpen()) {
          session.close();  // Close the WebSocket session
          System.out.println("WebSocket session closed.");
        }
      }
      // Clear the sessions list after closing
      connectedSessions.clear();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    connectedSessions.remove(session);
    System.out.println("WebSocket session closed: " + session.getId());
  }
}