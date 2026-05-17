package androidtoolkit.backend.device;

import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage.OperationResult;
import net.jqwik.api.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link CommandRelay}.
 * Covers Properties 6-9 from the design document.
 *
 * <p>Validates: Requirements 1.5, 4.1, 4.3, 4.4, 4.8
 */
class CommandRelayPropertyTest {

    private AgentConnectionManager createRealConnectionManager() {
        return new AgentConnectionManager();
    }

    private WebSocketSession mockSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private DeviceInfo createDevice(String serial) {
        return new DeviceInfo(serial, "TestMfg", "TestModel", "14",
                "", "", "192.168.1.1", "", false, "");
    }

    // --- Property 6: Command routing to correct agent ---

    /**
     * Property 6: Command routing to correct agent
     * For any device registered under a specific agent, commands for that device
     * are sent to that agent's session only.
     *
     * <p>Validates: Requirements 4.1
     */
    @Tag("Feature: agent-device-relay, Property 6: Command routing to correct agent")
    @Property(tries = 100)
    void commandRoutedToCorrectAgent(
            @ForAll("agentIds") String agentId,
            @ForAll("deviceSerials") String serial,
            @ForAll("tenantIds") Long tenantId) {

        // Arrange
        AgentConnectionManager connectionManager = createRealConnectionManager();
        WebSocketSession session = mockSession();
        connectionManager.registerAgent(agentId, tenantId, 1L, session);
        connectionManager.updateDeviceList(agentId, List.of(createDevice(serial)));

        CommandRelay relay = new CommandRelay(connectionManager);
        AgentCommand command = new AgentCommand.Reboot(serial);

        // Act - execute in a thread and complete the future from another thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<OperationResult> resultFuture = executor.submit(() ->
                relay.execute(tenantId, serial, command));

        // Give the execute method time to register the pending command and send
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify sendToAgent was called (which sends to the session)
        // The real AgentConnectionManager.sendToAgent sends a TextMessage to the session
        try {
            verify(session, atLeastOnce()).sendMessage(any());
        } catch (Exception e) {
            // sendMessage throws IOException
        }

        // Complete the pending command so the future resolves
        // Find the requestId from pending commands
        assertEquals(1, relay.getPendingCommandCount(),
                "Should have exactly one pending command");

        // Clean up
        relay.failCommandsForAgent(agentId);
        executor.shutdownNow();
    }

    // --- Property 7: Unreachable device returns error ---

    /**
     * Property 7: Unreachable device returns error
     * For any device serial with no agent registered, commands throw
     * DeviceUnreachableException with the serial.
     *
     * <p>Validates: Requirements 1.5, 4.3
     */
    @Tag("Feature: agent-device-relay, Property 7: Unreachable device returns error")
    @Property(tries = 100)
    void unreachableDeviceThrowsException(
            @ForAll("deviceSerials") String serial,
            @ForAll("tenantIds") Long tenantId) {

        // Arrange - no agent registered for this device
        AgentConnectionManager connectionManager = createRealConnectionManager();
        CommandRelay relay = new CommandRelay(connectionManager);
        AgentCommand command = new AgentCommand.Reboot(serial);

        // Act & Assert
        DeviceUnreachableException exception = assertThrows(
                DeviceUnreachableException.class,
                () -> relay.execute(tenantId, serial, command));

        assertEquals(serial, exception.getSerial(),
                "Exception should contain the device serial");
    }

    // --- Property 8: OperationResult completes pending command ---

    /**
     * Property 8: OperationResult completes pending command
     * For any command sent that produces an OperationResult, the future is
     * completed with the result.
     *
     * <p>Validates: Requirements 4.4
     */
    @Tag("Feature: agent-device-relay, Property 8: OperationResult completes pending command")
    @Property(tries = 100)
    void operationResultCompletesPendingCommand(
            @ForAll("agentIds") String agentId,
            @ForAll("deviceSerials") String serial,
            @ForAll("tenantIds") Long tenantId,
            @ForAll boolean success,
            @ForAll("detailMessages") String detail) throws InterruptedException, java.util.concurrent.ExecutionException {

        // Arrange
        AgentConnectionManager connectionManager = createRealConnectionManager();
        WebSocketSession session = mockSession();
        connectionManager.registerAgent(agentId, tenantId, 1L, session);
        connectionManager.updateDeviceList(agentId, List.of(createDevice(serial)));

        CommandRelay relay = new CommandRelay(connectionManager);
        AgentCommand command = new AgentCommand.Reboot(serial);

        // Act - execute in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<OperationResult> resultFuture = executor.submit(() ->
                relay.execute(tenantId, serial, command));

        // Wait for the command to be registered as pending
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Find the pending command's requestId by completing all pending commands
        // Since we know there's exactly one pending command, we can use completeCommand
        // We need to find the requestId - use reflection or iterate
        // Instead, we'll use failCommandsForAgent approach: complete via the relay's public API
        // The requestId is generated internally, so we need a different approach.
        // We'll use a spy on connectionManager to capture the requestId from the command sent.

        // Alternative approach: since getPendingCommandCount() == 1, we know there's one pending.
        // We can't easily get the requestId without accessing internals.
        // Let's use a different strategy: mock sendToAgent to capture the command, but since
        // we're using a real AgentConnectionManager, we'll capture via the session mock.

        // Actually, the simplest approach: iterate possible requestIds isn't feasible.
        // Let's restructure: use a spy on the connection manager to intercept sendToAgent
        // and extract the requestId from there. But AgentCommand.Reboot doesn't have a requestId.

        // The requestId is internal to CommandRelay. We need to complete it.
        // Best approach: use failCommandsForAgent to complete exceptionally, OR
        // use a custom approach where we intercept the pending commands map.

        // Simplest valid approach: since we can't get the requestId externally,
        // let's test this differently - we'll create a scenario where we know the requestId.
        // Actually, looking at the CommandRelay code more carefully, the requestId is a UUID
        // generated internally. The only way to complete it is via completeCommand(requestId, result).
        // We need to capture it somehow.

        // Let's use a wrapper approach: spy on the connection manager's sendToAgent
        // The command itself doesn't carry the requestId. The requestId is only in pendingCommands map.

        // Best approach for testing: We'll access the pending commands indirectly.
        // Since there's exactly 1 pending command, we can try all UUIDs... no that's not feasible.

        // Actually, let's look at this from a different angle. We can test this by:
        // 1. Starting execute in a thread
        // 2. Polling getPendingCommandCount until it's 1
        // 3. Using Java reflection to get the requestId from the pendingCommands map
        // OR we can test the completeCommand method directly without going through execute.

        // The cleanest approach: test completeCommand directly with a known requestId
        // by using reflection to insert a pending command, or by testing the integration.

        // Let me use reflection to access the pendingCommands map
        try {
            var field = CommandRelay.class.getDeclaredField("pendingCommands");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<String, ?> pendingCommands =
                    (ConcurrentHashMap<String, ?>) field.get(relay);

            // Wait until the pending command appears
            int attempts = 0;
            while (pendingCommands.isEmpty() && attempts < 20) {
                try { Thread.sleep(10); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                attempts++;
            }

            assertFalse(pendingCommands.isEmpty(), "Should have a pending command");

            // Get the requestId
            String requestId = pendingCommands.keySet().iterator().next();

            // Complete the command with the result
            OperationResult expectedResult = new OperationResult(requestId, success, detail);
            relay.completeCommand(requestId, expectedResult);

            // Verify the future completes with the correct result
            OperationResult actualResult = resultFuture.get(5, TimeUnit.SECONDS);
            assertEquals(success, actualResult.success(),
                    "Result success should match the OperationResult");
            assertEquals(detail, actualResult.detail(),
                    "Result detail should match the OperationResult");
            assertEquals(requestId, actualResult.requestId(),
                    "Result requestId should match");

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Could not access pendingCommands field: " + e.getMessage());
        } catch (TimeoutException e) {
            fail("Future did not complete within timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting");
        } catch (ExecutionException e) {
            fail("Future completed exceptionally: " + e.getCause().getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    // --- Property 9: Agent disconnect fails all pending commands ---

    /**
     * Property 9: Agent disconnect fails all pending commands
     * For any pending commands targeting a disconnected agent, all futures
     * complete exceptionally.
     *
     * <p>Validates: Requirements 4.8
     */
    @Tag("Feature: agent-device-relay, Property 9: Agent disconnect fails all pending commands")
    @Property(tries = 100)
    void agentDisconnectFailsAllPendingCommands(
            @ForAll("agentIds") String agentId,
            @ForAll("deviceSerialLists") List<String> serials,
            @ForAll("tenantIds") Long tenantId) {

        // Arrange
        AgentConnectionManager connectionManager = createRealConnectionManager();
        WebSocketSession session = mockSession();
        connectionManager.registerAgent(agentId, tenantId, 1L, session);

        List<DeviceInfo> devices = serials.stream()
                .map(this::createDevice)
                .toList();
        connectionManager.updateDeviceList(agentId, devices);

        CommandRelay relay = new CommandRelay(connectionManager);

        // Start commands for each device in separate threads
        ExecutorService executor = Executors.newFixedThreadPool(serials.size() + 1);
        List<Future<OperationResult>> futures = serials.stream()
                .map(serial -> executor.submit(() ->
                        relay.execute(tenantId, serial, new AgentCommand.Reboot(serial))))
                .toList();

        // Wait for all commands to be registered as pending
        try {
            int attempts = 0;
            while (relay.getPendingCommandCount() < serials.size() && attempts < 50) {
                Thread.sleep(10);
                attempts++;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertEquals(serials.size(), relay.getPendingCommandCount(),
                "All commands should be pending");

        // Act - simulate agent disconnect
        relay.failCommandsForAgent(agentId);

        // Assert - all futures should complete exceptionally with AgentDisconnectedException
        for (int i = 0; i < futures.size(); i++) {
            Future<OperationResult> future = futures.get(i);
            try {
                future.get(5, TimeUnit.SECONDS);
                fail("Future for device " + serials.get(i) + " should have completed exceptionally");
            } catch (ExecutionException e) {
                // The CompletionException wraps our AgentDisconnectedException
                Throwable cause = e.getCause();
                // CommandRelay.execute catches CompletionException and re-throws the cause
                assertTrue(cause instanceof AgentDisconnectedException,
                        "Should throw AgentDisconnectedException but got: " + cause.getClass().getName());
                AgentDisconnectedException ade = (AgentDisconnectedException) cause;
                assertEquals(agentId, ade.getAgentId(),
                        "Exception should reference the disconnected agent");
            } catch (TimeoutException e) {
                fail("Future for device " + serials.get(i) + " did not complete within timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting for future");
            }
        }

        executor.shutdownNow();
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> agentIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(15)
                .map(s -> "agent-" + s);
    }

    @Provide
    Arbitrary<String> deviceSerials() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> s + ":5555");
    }

    @Provide
    Arbitrary<Long> tenantIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<String> detailMessages() {
        return Arbitraries.strings()
                .ascii()
                .ofMinLength(0)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<List<String>> deviceSerialLists() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> s + ":5555")
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements();
    }
}
