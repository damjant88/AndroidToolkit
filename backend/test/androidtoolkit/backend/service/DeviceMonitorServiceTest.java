package androidtoolkit.backend.service;

import androidtoolkit.app.ConnectedDevice;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.domain.DeviceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeviceMonitorServiceTest {

    private DeviceCatalog deviceCatalog;
    private SimpMessagingTemplate messagingTemplate;
    private LogcatStreamManager logcatStreamManager;
    private DeviceMonitorService service;

    @BeforeEach
    void setUp() {
        deviceCatalog = mock(DeviceCatalog.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        logcatStreamManager = mock(LogcatStreamManager.class);
        service = new DeviceMonitorService(deviceCatalog, messagingTemplate, logcatStreamManager);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private DeviceInfo deviceInfo(String serial, String pid) {
        return new DeviceInfo(serial, "Samsung", "Galaxy S21", "14",
                "192.168.1.1", "", "192.168.1.1", "com.safepath", true, pid);
    }

    private ConnectedDevice connectedDevice(int index, String serial, String pid) {
        DeviceInfo info = deviceInfo(serial, pid);
        return new ConnectedDevice(index, serial, "Device" + (index + 1), info);
    }

    private DeviceDiscoveryResult discoveryResult(List<String> serials, List<ConnectedDevice> devices) {
        return new DeviceDiscoveryResult(serials, devices);
    }

    // ─── checkForDeviceChanges calls onDevicesChanged ────────────────────────────

    @Test
    @DisplayName("checkForDeviceChanges calls onDevicesChanged with correct DeviceInfo list")
    void checkForDeviceChanges_callsOnDevicesChangedWithCorrectDeviceInfoList() {
        ConnectedDevice dev1 = connectedDevice(0, "ABC123", "1001");
        ConnectedDevice dev2 = connectedDevice(1, "DEF456", "2002");
        DeviceDiscoveryResult result = discoveryResult(
                List.of("ABC123", "DEF456"),
                List.of(dev1, dev2)
        );
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(result);

        service.checkForDeviceChanges();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeviceInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(logcatStreamManager).onDevicesChanged(captor.capture());

        List<DeviceInfo> captured = captor.getValue();
        assertEquals(2, captured.size());
        assertEquals("ABC123", captured.get(0).getSerialNumber());
        assertEquals("1001", captured.get(0).getPid());
        assertEquals("DEF456", captured.get(1).getSerialNumber());
        assertEquals("2002", captured.get(1).getPid());
    }

    // ─── checkForDeviceChanges swallows LogcatStreamManager exceptions ───────────

    @Test
    @DisplayName("checkForDeviceChanges does NOT propagate exceptions from LogcatStreamManager")
    void checkForDeviceChanges_doesNotPropagateLogcatStreamManagerExceptions() {
        ConnectedDevice dev = connectedDevice(0, "SERIAL1", "100");
        DeviceDiscoveryResult result = discoveryResult(List.of("SERIAL1"), List.of(dev));
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(result);
        doThrow(new RuntimeException("Stream failure")).when(logcatStreamManager).onDevicesChanged(anyList());

        assertDoesNotThrow(() -> service.checkForDeviceChanges());
    }

    // ─── broadcastDeviceUpdate calls onDevicesChanged ────────────────────────────

    @Test
    @DisplayName("broadcastDeviceUpdate calls onDevicesChanged with correct DeviceInfo list")
    void broadcastDeviceUpdate_callsOnDevicesChangedWithCorrectDeviceInfoList() {
        ConnectedDevice dev1 = connectedDevice(0, "XYZ789", "3003");
        DeviceDiscoveryResult result = discoveryResult(List.of("XYZ789"), List.of(dev1));
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(result);

        service.broadcastDeviceUpdate();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DeviceInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(logcatStreamManager).onDevicesChanged(captor.capture());

        List<DeviceInfo> captured = captor.getValue();
        assertEquals(1, captured.size());
        assertEquals("XYZ789", captured.get(0).getSerialNumber());
        assertEquals("3003", captured.get(0).getPid());
    }

    // ─── broadcastDeviceUpdate swallows LogcatStreamManager exceptions ───────────

    @Test
    @DisplayName("broadcastDeviceUpdate does NOT propagate exceptions from LogcatStreamManager")
    void broadcastDeviceUpdate_doesNotPropagateLogcatStreamManagerExceptions() {
        ConnectedDevice dev = connectedDevice(0, "SERIAL2", "200");
        DeviceDiscoveryResult result = discoveryResult(List.of("SERIAL2"), List.of(dev));
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(result);
        doThrow(new RuntimeException("Stream failure")).when(logcatStreamManager).onDevicesChanged(anyList());

        assertDoesNotThrow(() -> service.broadcastDeviceUpdate());
    }

    // ─── checkForDeviceChanges broadcasts via WebSocket on serial change ─────────

    @Test
    @DisplayName("checkForDeviceChanges broadcasts device update via WebSocket when serials change")
    void checkForDeviceChanges_broadcastsViaWebSocketWhenSerialsChange() {
        ConnectedDevice dev = connectedDevice(0, "NEW_DEVICE", "500");
        DeviceDiscoveryResult result = discoveryResult(List.of("NEW_DEVICE"), List.of(dev));
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(result);

        service.checkForDeviceChanges();

        verify(messagingTemplate).convertAndSend(eq("/topic/devices"), eq(result));
    }
}
