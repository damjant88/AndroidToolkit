package androidtoolkit.backend.device;

import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.backend.config.GlobalExceptionHandler;
import androidtoolkit.backend.controller.DeviceController;
import androidtoolkit.backend.security.TenantContext;
import androidtoolkit.backend.service.LogcatStreamManager;
import androidtoolkit.service.CommandExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link DeviceController} with {@link DeviceProvider}.
 * Tests delegation to the provider and error handling via GlobalExceptionHandler.
 *
 * <p>Validates: Requirements 1.6, 7.5, 7.6
 */
class DeviceControllerTest {

    private DeviceProvider deviceProvider;
    private CommandExecutor commandExecutor;
    private LogcatStreamManager logcatStreamManager;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        deviceProvider = mock(DeviceProvider.class);
        commandExecutor = mock(CommandExecutor.class);
        logcatStreamManager = mock(LogcatStreamManager.class);

        DeviceController controller = new DeviceController(
                deviceProvider, commandExecutor, logcatStreamManager);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ─── Standalone mode: delegation to local provider ───────────────────────────

    @Nested
    @DisplayName("Standalone mode - delegation to DeviceProvider")
    class StandaloneDelegation {

        @Test
        @DisplayName("getConnectedDevices delegates to deviceProvider.discoverDevices with tenant ID")
        void getConnectedDevices_delegatesToProvider() throws Exception {
            Long tenantId = 42L;
            TenantContext.setTenantId(tenantId);

            DeviceDiscoveryResult expectedResult = new DeviceDiscoveryResult(
                    List.of("ABC123", "DEF456"), List.of());
            when(deviceProvider.discoverDevices(tenantId)).thenReturn(expectedResult);

            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serials[0]").value("ABC123"))
                    .andExpect(jsonPath("$.serials[1]").value("DEF456"));

            verify(deviceProvider).discoverDevices(tenantId);
        }

        @Test
        @DisplayName("getConnectedDevices with null tenant ID (standalone mode)")
        void getConnectedDevices_nullTenantId() throws Exception {
            // In standalone mode, TenantContext is not set (returns null)
            DeviceDiscoveryResult expectedResult = new DeviceDiscoveryResult(
                    List.of("SERIAL1"), List.of());
            when(deviceProvider.discoverDevices(null)).thenReturn(expectedResult);

            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serials[0]").value("SERIAL1"));

            verify(deviceProvider).discoverDevices(null);
        }

        @Test
        @DisplayName("reboot delegates to deviceProvider.reboot with tenant ID and serial")
        void reboot_delegatesToProvider() throws Exception {
            Long tenantId = 10L;
            TenantContext.setTenantId(tenantId);
            String serial = "DEVICE001";

            DeviceMessageResult expectedResult = new DeviceMessageResult("Rebooting device");
            when(deviceProvider.reboot(tenantId, serial)).thenReturn(expectedResult);

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Rebooting device"));

            verify(deviceProvider).reboot(tenantId, serial);
        }
    }

    // ─── SaaS mode: delegation to agent provider ─────────────────────────────────

    @Nested
    @DisplayName("SaaS mode - delegation to AgentDeviceProvider")
    class SaasDelegation {

        @Test
        @DisplayName("discoverDevices in SaaS mode returns agent-reported devices")
        void discoverDevices_saasMode() throws Exception {
            Long tenantId = 100L;
            TenantContext.setTenantId(tenantId);

            DeviceDiscoveryResult expectedResult = new DeviceDiscoveryResult(
                    List.of("REMOTE1", "REMOTE2"), List.of());
            when(deviceProvider.discoverDevices(tenantId)).thenReturn(expectedResult);

            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.serials[0]").value("REMOTE1"))
                    .andExpect(jsonPath("$.serials[1]").value("REMOTE2"));

            verify(deviceProvider).discoverDevices(tenantId);
        }

        @Test
        @DisplayName("reboot in SaaS mode relays command via provider")
        void reboot_saasMode() throws Exception {
            Long tenantId = 200L;
            TenantContext.setTenantId(tenantId);
            String serial = "AGENT_DEVICE_01";

            DeviceMessageResult expectedResult = new DeviceMessageResult("Reboot command sent");
            when(deviceProvider.reboot(tenantId, serial)).thenReturn(expectedResult);

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Reboot command sent"));

            verify(deviceProvider).reboot(tenantId, serial);
        }
    }

    // ─── Error responses ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error responses via GlobalExceptionHandler")
    class ErrorResponses {

        @Test
        @DisplayName("DeviceUnreachableException returns 503 with structured error")
        void deviceUnreachable_returns503() throws Exception {
            Long tenantId = 1L;
            TenantContext.setTenantId(tenantId);
            String serial = "UNREACHABLE01";

            when(deviceProvider.discoverDevices(tenantId))
                    .thenThrow(new DeviceUnreachableException(serial, "No agent connected"));

            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("device_unreachable"))
                    .andExpect(jsonPath("$.serial").value(serial))
                    .andExpect(jsonPath("$.reason").value("No agent connected"));
        }

        @Test
        @DisplayName("CommandTimeoutException returns 504 with structured error")
        void commandTimeout_returns504() throws Exception {
            Long tenantId = 2L;
            TenantContext.setTenantId(tenantId);
            String serial = "TIMEOUT_DEV";

            when(deviceProvider.reboot(tenantId, serial))
                    .thenThrow(new CommandTimeoutException(serial, 30));

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.error").value("command_timeout"))
                    .andExpect(jsonPath("$.serial").value(serial))
                    .andExpect(jsonPath("$.timeoutSeconds").value(30));
        }

        @Test
        @DisplayName("AgentDisconnectedException returns 502 with structured error")
        void agentDisconnected_returns502() throws Exception {
            Long tenantId = 3L;
            TenantContext.setTenantId(tenantId);
            String serial = "DISCONN_DEV";
            String agentId = "agent-xyz";

            when(deviceProvider.reboot(tenantId, serial))
                    .thenThrow(new AgentDisconnectedException(serial, agentId));

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("agent_disconnected"))
                    .andExpect(jsonPath("$.serial").value(serial))
                    .andExpect(jsonPath("$.agentId").value(agentId));
        }

        @Test
        @DisplayName("UnsupportedCommandException returns 400 with structured error")
        void unsupportedCommand_returns400() throws Exception {
            Long tenantId = 4L;
            TenantContext.setTenantId(tenantId);
            String serial = "DEVICE_X";

            when(deviceProvider.reboot(tenantId, serial))
                    .thenThrow(new UnsupportedCommandException("wifi-debug"));

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("unsupported_command"))
                    .andExpect(jsonPath("$.commandType").value("wifi-debug"));
        }

        @Test
        @DisplayName("DeviceUnreachableException on reboot returns 503")
        void reboot_deviceUnreachable_returns503() throws Exception {
            Long tenantId = 5L;
            TenantContext.setTenantId(tenantId);
            String serial = "NO_AGENT_DEV";

            when(deviceProvider.reboot(tenantId, serial))
                    .thenThrow(new DeviceUnreachableException(serial, "Device not found in any agent"));

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("device_unreachable"))
                    .andExpect(jsonPath("$.serial").value(serial))
                    .andExpect(jsonPath("$.reason").value("Device not found in any agent"));
        }

        @Test
        @DisplayName("Uninstall with timeout returns 504")
        void uninstall_timeout_returns504() throws Exception {
            Long tenantId = 6L;
            TenantContext.setTenantId(tenantId);
            String serial = "SLOW_DEV";

            when(deviceProvider.uninstall(tenantId, serial, "com.example.app"))
                    .thenThrow(new CommandTimeoutException(serial, 30));

            mockMvc.perform(post("/api/devices/{serial}/uninstall", serial)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"packageName\": \"com.example.app\"}"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.error").value("command_timeout"))
                    .andExpect(jsonPath("$.serial").value(serial));
        }
    }

    // ─── Cross-tenant rejection ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Cross-tenant rejection")
    class CrossTenantRejection {

        @Test
        @DisplayName("Provider throwing DeviceUnreachableException for cross-tenant access returns 503")
        void crossTenantAccess_providerRejectsWithUnreachable() throws Exception {
            // In the current architecture, cross-tenant rejection happens at the
            // DeviceProvider level (AgentDeviceProvider won't find the device for
            // the wrong tenant), resulting in DeviceUnreachableException.
            Long tenantId = 99L;
            TenantContext.setTenantId(tenantId);
            String serial = "OTHER_TENANT_DEV";

            when(deviceProvider.reboot(tenantId, serial))
                    .thenThrow(new DeviceUnreachableException(serial,
                            "No agent connected for tenant"));

            mockMvc.perform(post("/api/devices/{serial}/reboot", serial))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("device_unreachable"))
                    .andExpect(jsonPath("$.serial").value(serial));
        }
    }
}
