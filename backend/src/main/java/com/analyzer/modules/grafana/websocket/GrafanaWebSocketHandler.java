package com.analyzer.modules.grafana.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages WebSocket connections for real-time Grafana dashboard updates.
 *
 * Architecture decisions:
 * - CopyOnWriteArraySet for the session registry because writes (connect/disconnect)
 *   are infrequent relative to reads (broadcasts), making COW optimal.
 * - Broadcasting uses snapshot iteration so a disconnecting client doesn't break the loop.
 * - Max message size enforced at 64KB to prevent abuse.
 * - Uses Spring-managed ObjectMapper for consistent serialization config.
 */
@Slf4j
@Component
public class GrafanaWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_SIZE = 64 * 1024;

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    public GrafanaWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket connected: {} (total: {})", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket disconnected: {} (total: {})", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (message.getPayloadLength() > MAX_MESSAGE_SIZE) {
            log.warn("WebSocket message too large from session {}: {} bytes",
                    session.getId(), message.getPayloadLength());
            return;
        }
        log.trace("WebSocket message from {}: {} bytes",
                session.getId(), message.getPayloadLength());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception);
        sessions.remove(session);
    }

    /**
     * Broadcasts a JSON message to all connected WebSocket clients.
     * Serializes once and reuses the same TextMessage for all clients.
     */
    public void broadcast(String topic, Object payload) {
        if (sessions.isEmpty()) return;

        try {
            var message = new WebSocketMessage<>(topic, payload);
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.debug("Failed to send to session {}: {}", session.getId(), e.getMessage());
                        sessions.remove(session);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast {}: {}", topic, e.getMessage());
        }
    }

    public int getActiveConnections() {
        return sessions.size();
    }

    /**
     * Envelope wrapping all WebSocket messages with a topic discriminator.
     */
    public record WebSocketMessage<T>(String topic, T data) {}
}
