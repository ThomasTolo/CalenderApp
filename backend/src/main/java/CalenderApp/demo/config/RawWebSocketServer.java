package CalenderApp.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Set;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

// Inspiration: https://docs.spring.io/spring-framework/docs/4.3.15.RELEASE/spring-framework-reference/html/websocket.html#websocket-server

@Configuration
@EnableWebSocket
public class RawWebSocketServer implements WebSocketConfigurer {
    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(new SimpleTextHandler(), "/rawws").setAllowedOrigins("*");
    }

    public static void broadcast(@NonNull String message) {
        String safeMessage = Objects.requireNonNull(message, "message");
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new org.springframework.web.socket.TextMessage(safeMessage));
            } catch (Exception ignored) { }
        }
    }

    public static void broadcastJson(Object payload) {
        try {
            broadcast(Objects.requireNonNull(MAPPER.writeValueAsString(payload)));
        } catch (Exception ignored) { }
    }

    private static class SimpleTextHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
            sessions.add(session);
        }

        @Override
        public void afterConnectionClosed(
                @NonNull WebSocketSession session,
                @NonNull org.springframework.web.socket.CloseStatus status
        ) throws Exception {
            sessions.remove(session);
        }

        @Override
        public void handleTextMessage(
                @NonNull WebSocketSession session,
                @NonNull org.springframework.web.socket.TextMessage message
        ) throws Exception {
            session.sendMessage(new org.springframework.web.socket.TextMessage(Objects.requireNonNull(message.getPayload(), "")));
        }
    }
}
