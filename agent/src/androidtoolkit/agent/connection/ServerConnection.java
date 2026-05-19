package androidtoolkit.agent.connection;

import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ServerConnection.class);

    @Value("${agent.server-url:ws://localhost:8080/ws/agent}")
    private String serverUrl;

    @Value("${agent.token:}")
    private String authToken;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Queue<String> pendingMessages = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private WebSocketSession session;
    private Consumer<AgentCommand> commandHandler;
    private java.util.function.BiConsumer<AgentCommand, String> commandHandlerWithRequestId;
    private Runnable onReconnected;
    private int consecutiveFailures = 0;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000;
    private static final double BACKOFF_FACTOR = 2.0;

    public void setCommandHandler(Consumer<AgentCommand> handler) {
        this.commandHandler = handler;
    }

    /**
     * Sets a command handler that also receives the requestId from the command envelope.
     */
    public void setCommandHandlerWithRequestId(java.util.function.BiConsumer<AgentCommand, String> handler) {
        this.commandHandlerWithRequestId = handler;
    }

    /**
     * Sets a callback to be invoked after a successful (re)connection.
     * The agent application uses this to re-send the current DeviceList
     * within 5 seconds of connection establishment.
     */
    public void setOnReconnected(Runnable onReconnected) {
        this.onReconnected = onReconnected;
    }

    @PostConstruct
    public void connect() {
        attemptConnection();
    }

    private void attemptConnection() {
        try {
            String url = serverUrl + "?token=" + authToken;
            log.info("Connecting to server: {} (token length: {})", serverUrl, authToken.length());
            var client = new StandardWebSocketClient();
            this.session = client.execute(this, url).get(10, TimeUnit.SECONDS);
            // Allow large messages (screenshots as base64 can be 5-10MB)
            this.session.setTextMessageSizeLimit(15 * 1024 * 1024);
            this.session.setBinaryMessageSizeLimit(15 * 1024 * 1024);
            consecutiveFailures = 0;
            log.info("Connected to server successfully");
            flushPendingMessages();
            if (onReconnected != null) {
                onReconnected.run();
            }
        } catch (Exception e) {
            consecutiveFailures++;
            long delay = calculateBackoffDelay();
            log.warn("Connection attempt failed (attempt {}), retrying in {}ms: {}", 
                    consecutiveFailures, delay, e.getMessage());
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
        if (commandHandlerWithRequestId != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(message.getPayload());
                String requestId = node.has("requestId") ? node.get("requestId").asText(null) : null;
                AgentCommand command = objectMapper.treeToValue(node, AgentCommand.class);
                commandHandlerWithRequestId.accept(command, requestId);
            } catch (Exception e) {
                // Log parsing error
            }
        } else if (commandHandler != null) {
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
        log.warn("Connection closed: {}. Reconnecting...", status);
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
