package androidtoolkit.agent.connection;

import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;

import jakarta.annotation.PostConstruct;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket client that connects to the Server with exponential backoff reconnection.
 */
@Component
public class ServerConnection extends TextWebSocketHandler {

    @Value("${agent.server-url:ws://localhost:8080/ws/agent}")
    private String serverUrl;

    @Value("${agent.token:}")
    private String authToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private Consumer<AgentCommand> commandHandler;
    private int consecutiveFailures = 0;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000;
    private static final double BACKOFF_FACTOR = 2.0;

    public void setCommandHandler(Consumer<AgentCommand> handler) {
        this.commandHandler = handler;
    }

    @PostConstruct
    public void connect() {
        attemptConnection();
    }

    private void attemptConnection() {
        try {
            String url = serverUrl + "?token=" + authToken;
            var client = new StandardWebSocketClient();
            this.session = client.execute(this, url).get(10, TimeUnit.SECONDS);
            consecutiveFailures = 0;
            flushPendingMessages();
        } catch (Exception e) {
            consecutiveFailures++;
            long delay = calculateBackoffDelay();
            scheduler.schedule(this::attemptConnection, delay, TimeUnit.MILLISECONDS);
        }
    }

    public void send(AgentMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            } else {
                pendingMessages.offer(json);
            }
        } catch (Exception e) {
            // Buffer for retry
            try {
                pendingMessages.offer(objectMapper.writeValueAsString(message));
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (commandHandler != null) {
            try {
                AgentCommand command = objectMapper.readValue(message.getPayload(), AgentCommand.class);
                commandHandler.accept(command);
            } catch (Exception e) {
                // Log parsing error
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.session = null;
        consecutiveFailures++;
        long delay = calculateBackoffDelay();
        scheduler.schedule(this::attemptConnection, delay, TimeUnit.MILLISECONDS);
    }

    private void flushPendingMessages() {
        String msg;
        while ((msg = pendingMessages.poll()) != null) {
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(msg));
                } else {
                    pendingMessages.offer(msg);
                    break;
                }
            } catch (Exception e) {
                pendingMessages.offer(msg);
                break;
            }
        }
    }

    long calculateBackoffDelay() {
        long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_FACTOR, consecutiveFailures - 1));
        return Math.min(delay, MAX_DELAY_MS);
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }
}
