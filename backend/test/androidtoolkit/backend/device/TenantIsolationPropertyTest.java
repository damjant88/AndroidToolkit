package androidtoolkit.backend.device;

import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.domain.agent.AgentCommand;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for cross-tenant command rejection.
 * Covers Property 10 from the design document.
 *
 * <p>Validates: Requirements 7.5, 7.6
 */
class TenantIsolationPropertyTest {

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

    // --- Property 10: Cross-tenant command rejection ---

    /**
     * Property 10: Cross-tenant command rejection
     * For any command request where the authenticated user's tenant ID differs from
     * the tenant that owns the target device, the system rejects the request with an
     * error and does NOT forward the command to any agent.
     *
     * <p>Validates: Requirements 7.5, 7.6
     */
    @Tag("Feature: agent-device-relay, Property 10: Cross-tenant command rejection")
    @Property(tries = 100)
    void crossTenantCommandIsRejected(
            @ForAll("agentIds") String agentId,
            @ForAll("deviceSerials") String serial,
            @ForAll("distinctTenantPairs") Long[] tenantPair) {

        Long ownerTenantId = tenantPair[0];
        Long attackerTenantId = tenantPair[1];

        // Arrange: register an agent for tenant A with a device
        AgentConnectionManager connectionManager = createRealConnectionManager();
        WebSocketSession session = mockSession();
        connectionManager.registerAgent(agentId, ownerTenantId, 1L, session);
        connectionManager.updateDeviceList(agentId, List.of(createDevice(serial)));

        CommandRelay relay = new CommandRelay(connectionManager);
        AgentCommand command = new AgentCommand.Reboot(serial);

        // Act & Assert: executing with tenant B's ID should throw DeviceUnreachableException
        DeviceUnreachableException exception = assertThrows(
                DeviceUnreachableException.class,
                () -> relay.execute(attackerTenantId, serial, command),
                "Cross-tenant command should be rejected");

        // Verify the exception contains the correct serial
        assertEquals(serial, exception.getSerial(),
                "Exception should reference the target device serial");

        // Verify no command was sent to any agent (no sendMessage on the session)
        try {
            verify(session, never()).sendMessage(any());
        } catch (Exception e) {
            fail("Unexpected exception verifying no message sent: " + e.getMessage());
        }
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

    /**
     * Generates a pair of distinct tenant IDs [ownerTenantId, attackerTenantId]
     * where ownerTenantId != attackerTenantId.
     */
    @Provide
    Arbitrary<Long[]> distinctTenantPairs() {
        return Arbitraries.longs().between(1L, 1000L)
                .flatMap(owner -> Arbitraries.longs().between(1L, 1000L)
                        .filter(attacker -> !attacker.equals(owner))
                        .map(attacker -> new Long[]{owner, attacker}));
    }
}
