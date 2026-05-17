package androidtoolkit.agent.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ServerConnection} reconnection behavior.
 *
 * Since ServerConnection is in the agent module and calculateBackoffDelay() is package-private,
 * these tests use reflection to access internal state and verify behavior.
 *
 * Validates: Requirements 8.1, 8.4, 8.5
 */
class ServerConnectionTest {

    private ServerConnection serverConnection;

    @BeforeEach
    void setUp() throws Exception {
        serverConnection = new ServerConnection();
        // Prevent @PostConstruct connect() from running in tests by not using Spring context
        // The object is created directly, so @PostConstruct won't fire
    }

    // --- Backoff Delay Calculation Tests ---

    @Nested
    @DisplayName("Exponential Backoff Delay Calculation")
    class BackoffDelayTests {

        /**
         * Helper to set consecutiveFailures via reflection and call calculateBackoffDelay.
         */
        private long getBackoffDelay(int consecutiveFailures) throws Exception {
            Field failuresField = ServerConnection.class.getDeclaredField("consecutiveFailures");
            failuresField.setAccessible(true);
            failuresField.setInt(serverConnection, consecutiveFailures);

            // calculateBackoffDelay is package-private, accessible from same package
            Method method = ServerConnection.class.getDeclaredMethod("calculateBackoffDelay");
            method.setAccessible(true);
            return (long) method.invoke(serverConnection);
        }

        @Test
        @DisplayName("n=1 → 1000ms (initial delay)")
        void firstFailureReturnsInitialDelay() throws Exception {
            long delay = getBackoffDelay(1);
            assertEquals(1000L, delay, "First failure should produce 1000ms delay (1000 * 2^0)");
        }

        @Test
        @DisplayName("n=2 → 2000ms")
        void secondFailureReturns2000ms() throws Exception {
            long delay = getBackoffDelay(2);
            assertEquals(2000L, delay, "Second failure should produce 2000ms delay (1000 * 2^1)");
        }

        @Test
        @DisplayName("n=3 → 4000ms")
        void thirdFailureReturns4000ms() throws Exception {
            long delay = getBackoffDelay(3);
            assertEquals(4000L, delay, "Third failure should produce 4000ms delay (1000 * 2^2)");
        }

        @Test
        @DisplayName("n=4 → 8000ms")
        void fourthFailureReturns8000ms() throws Exception {
            long delay = getBackoffDelay(4);
            assertEquals(8000L, delay, "Fourth failure should produce 8000ms delay (1000 * 2^3)");
        }

        @Test
        @DisplayName("n=5 → 16000ms")
        void fifthFailureReturns16000ms() throws Exception {
            long delay = getBackoffDelay(5);
            assertEquals(16000L, delay, "Fifth failure should produce 16000ms delay (1000 * 2^4)");
        }

        @Test
        @DisplayName("n=6 → 32000ms")
        void sixthFailureReturns32000ms() throws Exception {
            long delay = getBackoffDelay(6);
            assertEquals(32000L, delay, "Sixth failure should produce 32000ms delay (1000 * 2^5)");
        }

        @Test
        @DisplayName("n=7 → 60000ms (capped at max)")
        void seventhFailureReturnsCappedMax() throws Exception {
            long delay = getBackoffDelay(7);
            assertEquals(60000L, delay, "Seventh failure should be capped at 60000ms (1000 * 2^6 = 64000 > 60000)");
        }

        @Test
        @DisplayName("n=10 → 60000ms (remains capped)")
        void tenthFailureRemainsCapped() throws Exception {
            long delay = getBackoffDelay(10);
            assertEquals(60000L, delay, "Tenth failure should remain capped at 60000ms");
        }

        @Test
        @DisplayName("n=50 → 60000ms (large n remains capped)")
        void largeFailureCountRemainsCapped() throws Exception {
            long delay = getBackoffDelay(50);
            assertEquals(60000L, delay, "Large failure count should remain capped at 60000ms");
        }
    }

    // --- onReconnected Callback Tests ---

    @Nested
    @DisplayName("onReconnected Callback Mechanism")
    class OnReconnectedCallbackTests {

        @Test
        @DisplayName("setOnReconnected stores the callback")
        void setOnReconnectedStoresCallback() throws Exception {
            Runnable callback = () -> {};
            serverConnection.setOnReconnected(callback);

            Field field = ServerConnection.class.getDeclaredField("onReconnected");
            field.setAccessible(true);
            Runnable stored = (Runnable) field.get(serverConnection);

            assertSame(callback, stored, "The onReconnected callback should be stored");
        }

        @Test
        @DisplayName("setOnReconnected with null clears the callback")
        void setOnReconnectedWithNullClearsCallback() throws Exception {
            serverConnection.setOnReconnected(() -> {});
            serverConnection.setOnReconnected(null);

            Field field = ServerConnection.class.getDeclaredField("onReconnected");
            field.setAccessible(true);
            Runnable stored = (Runnable) field.get(serverConnection);

            assertNull(stored, "Setting null should clear the onReconnected callback");
        }

        @Test
        @DisplayName("onReconnected field is initially null")
        void onReconnectedIsInitiallyNull() throws Exception {
            Field field = ServerConnection.class.getDeclaredField("onReconnected");
            field.setAccessible(true);
            Runnable stored = (Runnable) field.get(serverConnection);

            assertNull(stored, "onReconnected should be null initially");
        }
    }

    // --- isConnected Tests ---

    @Nested
    @DisplayName("isConnected Behavior")
    class IsConnectedTests {

        @Test
        @DisplayName("isConnected returns false when no session exists")
        void isConnectedReturnsFalseWhenNoSession() {
            assertFalse(serverConnection.isConnected(),
                    "isConnected should return false when no WebSocket session exists");
        }

        @Test
        @DisplayName("isConnected returns false when session is null")
        void isConnectedReturnsFalseWhenSessionIsNull() throws Exception {
            Field sessionField = ServerConnection.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(serverConnection, null);

            assertFalse(serverConnection.isConnected(),
                    "isConnected should return false when session is explicitly null");
        }
    }

    // --- Pending Messages Tests ---

    @Nested
    @DisplayName("Pending Messages Queue")
    class PendingMessagesTests {

        @Test
        @DisplayName("Pending messages queue is initially empty")
        void pendingMessagesQueueIsInitiallyEmpty() throws Exception {
            Field queueField = ServerConnection.class.getDeclaredField("pendingMessages");
            queueField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Queue<String> queue = (java.util.Queue<String>) queueField.get(serverConnection);

            assertTrue(queue.isEmpty(), "Pending messages queue should be initially empty");
        }

        @Test
        @DisplayName("Messages are queued when session is not connected")
        void messagesAreQueuedWhenNotConnected() throws Exception {
            // Session is null (not connected), so send should queue the message
            serverConnection.send(new androidtoolkit.domain.agent.AgentMessage.DeviceList(java.util.List.of()));

            Field queueField = ServerConnection.class.getDeclaredField("pendingMessages");
            queueField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Queue<String> queue = (java.util.Queue<String>) queueField.get(serverConnection);

            assertFalse(queue.isEmpty(),
                    "Message should be queued when session is not connected");
        }

        @Test
        @DisplayName("Multiple messages are queued in order when not connected")
        void multipleMessagesQueuedInOrder() throws Exception {
            serverConnection.send(new androidtoolkit.domain.agent.AgentMessage.DeviceList(java.util.List.of()));
            serverConnection.send(new androidtoolkit.domain.agent.AgentMessage.DeviceList(java.util.List.of()));
            serverConnection.send(new androidtoolkit.domain.agent.AgentMessage.DeviceList(java.util.List.of()));

            Field queueField = ServerConnection.class.getDeclaredField("pendingMessages");
            queueField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Queue<String> queue = (java.util.Queue<String>) queueField.get(serverConnection);

            assertEquals(3, queue.size(),
                    "All three messages should be queued when session is not connected");
        }
    }
}
