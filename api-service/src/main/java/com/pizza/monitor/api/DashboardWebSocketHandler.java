package com.pizza.monitor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final DashboardService dashboard;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, 5_000, 512 * 1024);
        sessions.put(session.getId(), safe);
        log.info("WebSocket client connected (clients: {})", sessions.size());

        try {
            send(safe, buildSnapshot());
        } catch (Exception e) {
            log.error("Failed to send initial snapshot", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @Nonnull CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket client disconnected (clients: {})", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: {}", exception.getMessage());
        sessions.remove(session.getId());
    }

    @Scheduled(fixedDelayString = "${pizza.api.broadcast-interval-ms:2000}")
    public void broadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        String snapshot;
        try {
            snapshot = buildSnapshot();
        } catch (Exception e) {
            log.error("WebSocket broadcast failed", e);
            return;
        }
        sessions.values().forEach(session -> {
            try {
                send(session, snapshot);
            } catch (Exception e) {
                log.warn("Dropping WebSocket client: {}", e.getMessage());
                sessions.remove(session.getId());
            }
        });
    }

    private String buildSnapshot() throws JsonProcessingException {
        return objectMapper.writeValueAsString(dashboard.snapshot());
    }

    private void send(WebSocketSession session, String payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload));
        }
    }
}
