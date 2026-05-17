package androidtoolkit.backend.device;

import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.backend.service.AgentConnectionManager;
import androidtoolkit.domain.DeviceInfo;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link AgentConnectionManager}.
 * Covers Properties 1-5 from the design document.
 *
 * Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.2, 3.5, 8.2, 8.3
 */
class AgentConnectionManagerPropertyTest {

    private AgentConnectionManager createManager() {
        return new AgentConnectionManager();
    }

    private WebSocketSession mockSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn(UUID.randomUUID().toString());
        return session;
    }

    // --- Property 1: DeviceList replacement is total and data-preserving ---

    /**
     * Property 1: DeviceList replacement is total and data-preserving
     *
     * For any agent with a previously registered device set, when a new DeviceList arrives,
     * the registry contains exactly the new devices (no more, no less), each retaining serial and model.
     *
     * Validates: Requirements 2.2, 2.6, 2.7, 8.2
     */
    @Tag("Feature: agent-device-relay, Property 1: DeviceList replacement is total and data-preserving")
    @Property(tries = 100)
    void deviceListReplacementIsTotalAndDataPreserving(
            @ForAll("deviceInfoList") List<DeviceInfo> initialDevices,
            @ForAll("deviceInfoList") List<DeviceInfo> newDevices) {

        AgentConnectionManager manager = createManager();
        String agentId = "agent-1";
        Long tenantId = 1L;

        manager.registerAgent(agentId, tenantId, 100L, mockSession());

        // Register initial devices
        manager.updateDeviceList(agentId, initialDevices);

        // Replace with new devices
        manager.updateDeviceList(agentId, newDevices);

        // Verify: registry contains exactly the new devices
        DeviceDiscoveryResult result = manager.getDevicesForTenant(tenantId);

        Set<String> expectedSerials = newDevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());

        Set<String> actualSerials = new HashSet<>(result.getSerials());

        assertEquals(expectedSerials, actualSerials,
                "After replacement, registry should contain exactly the new device serials");

        // Verify each device retains serial and model
        for (ConnectedDevice connectedDevice : result.getDevices()) {
            DeviceInfo deviceInfo = connectedDevice.getDeviceInfo();
            assertNotNull(deviceInfo.getSerialNumber(),
                    "Each stored device should retain its serial number");
            assertNotNull(deviceInfo.getModel(),
                    "Each stored device should retain its model");

            // Verify the device data matches one of the new devices
            boolean found = newDevices.stream().anyMatch(d ->
                    d.getSerialNumber().equals(deviceInfo.getSerialNumber()) &&
                    d.getModel().equals(deviceInfo.getModel()));
            assertTrue(found,
                    "Each stored device should match a device from the new list");
        }
    }

    // --- Property 2: Agent disconnect removes all devices ---

    /**
     * Property 2: Agent disconnect removes all devices
     *
     * For any agent with registered devices, when that agent disconnects,
     * zero devices remain for that agent.
     *
     * Validates: Requirements 2.3, 8.3
     */
    @Tag("Feature: agent-device-relay, Property 2: Agent disconnect removes all devices")
    @Property(tries = 100)
    void agentDisconnectRemovesAllDevices(
            @ForAll("deviceInfoList") List<DeviceInfo> devices) {

        AgentConnectionManager manager = createManager();
        String agentId = "agent-disconnect";
        Long tenantId = 2L;

        manager.registerAgent(agentId, tenantId, 200L, mockSession());
        manager.updateDeviceList(agentId, devices);

        // Disconnect the agent
        manager.unregisterAgent(agentId);

        // Verify: zero devices remain for that agent's tenant
        DeviceDiscoveryResult result = manager.getDevicesForTenant(tenantId);
        assertEquals(0, result.getDeviceCount(),
                "After agent disconnect, zero devices should remain for that agent");
        assertTrue(result.getSerials().isEmpty(),
                "After agent disconnect, no serials should remain");
    }

    // --- Property 3: Tenant isolation on device queries ---

    /**
     * Property 3: Tenant isolation on device queries
     *
     * For agents belonging to different tenants, querying devices for tenant X
     * returns only devices from tenant X's agents.
     *
     * Validates: Requirements 2.4, 2.5, 3.3, 7.4, 7.5
     */
    @Tag("Feature: agent-device-relay, Property 3: Tenant isolation on device queries")
    @Property(tries = 100)
    void tenantIsolationOnDeviceQueries(
            @ForAll("deviceInfoList") List<DeviceInfo> tenantADevices,
            @ForAll("deviceInfoList") List<DeviceInfo> tenantBDevices) {

        AgentConnectionManager manager = createManager();
        Long tenantA = 10L;
        Long tenantB = 20L;

        manager.registerAgent("agent-A", tenantA, 100L, mockSession());
        manager.registerAgent("agent-B", tenantB, 200L, mockSession());

        manager.updateDeviceList("agent-A", tenantADevices);
        manager.updateDeviceList("agent-B", tenantBDevices);

        // Query tenant A's devices
        DeviceDiscoveryResult resultA = manager.getDevicesForTenant(tenantA);
        Set<String> serialsA = new HashSet<>(resultA.getSerials());

        // Query tenant B's devices
        DeviceDiscoveryResult resultB = manager.getDevicesForTenant(tenantB);
        Set<String> serialsB = new HashSet<>(resultB.getSerials());

        // Verify tenant A only sees its own devices
        Set<String> expectedSerialsA = tenantADevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());
        assertEquals(expectedSerialsA, serialsA,
                "Tenant A should only see its own devices");

        // Verify tenant B only sees its own devices
        Set<String> expectedSerialsB = tenantBDevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());
        assertEquals(expectedSerialsB, serialsB,
                "Tenant B should only see its own devices");

        // Verify no cross-contamination: tenant A's devices should not appear in tenant B's results
        for (String serial : serialsA) {
            // Only check if the serial is unique to tenant A (not shared by coincidence in generated data)
            if (!expectedSerialsB.contains(serial)) {
                assertFalse(serialsB.contains(serial),
                        "Tenant A's unique device should not appear in tenant B's results");
            }
        }
    }

    // --- Property 4: Multi-agent device aggregation ---

    /**
     * Property 4: Multi-agent device aggregation
     *
     * For a tenant with multiple agents, the aggregated device list equals
     * the union of all agents' device serial sets.
     *
     * Validates: Requirements 3.5
     */
    @Tag("Feature: agent-device-relay, Property 4: Multi-agent device aggregation")
    @Property(tries = 100)
    void multiAgentDeviceAggregation(
            @ForAll("deviceInfoList") List<DeviceInfo> agent1Devices,
            @ForAll("deviceInfoList") List<DeviceInfo> agent2Devices,
            @ForAll("deviceInfoList") List<DeviceInfo> agent3Devices) {

        AgentConnectionManager manager = createManager();
        Long tenantId = 30L;

        manager.registerAgent("multi-agent-1", tenantId, 301L, mockSession());
        manager.registerAgent("multi-agent-2", tenantId, 302L, mockSession());
        manager.registerAgent("multi-agent-3", tenantId, 303L, mockSession());

        manager.updateDeviceList("multi-agent-1", agent1Devices);
        manager.updateDeviceList("multi-agent-2", agent2Devices);
        manager.updateDeviceList("multi-agent-3", agent3Devices);

        // Get aggregated result
        DeviceDiscoveryResult result = manager.getDevicesForTenant(tenantId);
        Set<String> actualSerials = new HashSet<>(result.getSerials());

        // Expected: union of all agents' device serials
        Set<String> expectedSerials = new HashSet<>();
        agent1Devices.stream().map(DeviceInfo::getSerialNumber).forEach(expectedSerials::add);
        agent2Devices.stream().map(DeviceInfo::getSerialNumber).forEach(expectedSerials::add);
        agent3Devices.stream().map(DeviceInfo::getSerialNumber).forEach(expectedSerials::add);

        assertEquals(expectedSerials, actualSerials,
                "Aggregated device list should equal the union of all agents' device serial sets");
    }

    // --- Property 5: Device change detection triggers broadcast ---

    /**
     * Property 5: Device change detection triggers broadcast
     *
     * When a DeviceList arrives with different serials, hasDeviceSetChanged returns true;
     * when identical, returns false.
     *
     * Validates: Requirements 3.2
     */
    @Tag("Feature: agent-device-relay, Property 5: Device change detection triggers broadcast")
    @Property(tries = 100)
    void deviceChangeDetectionWhenDifferent(
            @ForAll("deviceInfoList") List<DeviceInfo> initialDevices,
            @ForAll("deviceInfoList") List<DeviceInfo> newDevices) {

        AgentConnectionManager manager = createManager();
        String agentId = "agent-change";
        Long tenantId = 40L;

        manager.registerAgent(agentId, tenantId, 400L, mockSession());
        manager.updateDeviceList(agentId, initialDevices);

        Set<String> initialSerials = initialDevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());
        Set<String> newSerials = newDevices.stream()
                .map(DeviceInfo::getSerialNumber)
                .collect(Collectors.toSet());

        boolean changed = manager.hasDeviceSetChanged(agentId, newDevices);

        if (initialSerials.equals(newSerials)) {
            assertFalse(changed,
                    "When serial sets are identical, hasDeviceSetChanged should return false");
        } else {
            assertTrue(changed,
                    "When serial sets differ, hasDeviceSetChanged should return true");
        }
    }

    @Tag("Feature: agent-device-relay, Property 5: Device change detection triggers broadcast")
    @Property(tries = 100)
    void deviceChangeDetectionIdenticalReturnsFalse(
            @ForAll("deviceInfoList") List<DeviceInfo> devices) {

        AgentConnectionManager manager = createManager();
        String agentId = "agent-same";
        Long tenantId = 50L;

        manager.registerAgent(agentId, tenantId, 500L, mockSession());
        manager.updateDeviceList(agentId, devices);

        // Query with the same device list — should return false
        boolean changed = manager.hasDeviceSetChanged(agentId, devices);
        assertFalse(changed,
                "When the same device list is provided, hasDeviceSetChanged should return false");
    }

    // --- Arbitraries ---

    @Provide
    Arbitrary<List<DeviceInfo>> deviceInfoList() {
        return deviceInfoArbitrary().list().ofMinSize(0).ofMaxSize(8)
                .filter(list -> {
                    // Ensure unique serials within a list
                    Set<String> serials = list.stream()
                            .map(DeviceInfo::getSerialNumber)
                            .collect(Collectors.toSet());
                    return serials.size() == list.size();
                });
    }

    @Provide
    Arbitrary<DeviceInfo> deviceInfoArbitrary() {
        Arbitrary<String> serials = Arbitraries.strings()
                .alpha().numeric()
                .ofMinLength(6).ofMaxLength(12)
                .map(s -> "SN-" + s);

        Arbitrary<String> manufacturers = Arbitraries.of(
                "Samsung", "Google", "OnePlus", "Xiaomi", "Huawei", "Sony", "LG", "Motorola");

        Arbitrary<String> models = Arbitraries.of(
                "Pixel 7", "Galaxy S23", "OnePlus 11", "Mi 13", "P60 Pro",
                "Xperia 1 V", "G8 ThinQ", "Edge 40");

        Arbitrary<String> osVersions = Arbitraries.of(
                "11", "12", "13", "14", "15");

        Arbitrary<String> ips = Arbitraries.of(
                "192.168.1.100", "10.0.0.5", "172.16.0.1", "");

        Arbitrary<Boolean> booleans = Arbitraries.of(true, false);

        Arbitrary<String> pids = Arbitraries.of("1234", "5678", "9012", "");

        // Use combine with 8 params (max supported) and hardcode safePathPackage
        return Combinators.combine(serials, manufacturers, models, osVersions, ips, ips, ips, booleans)
                .flatAs((serial, manufacturer, model, osVersion, wifiIp, mobileIp, ipAddress, appInstalled) ->
                        pids.map(pid -> new DeviceInfo(serial, manufacturer, model, osVersion,
                                wifiIp, mobileIp, ipAddress, "com.example.app", appInstalled, pid)));
    }
}
