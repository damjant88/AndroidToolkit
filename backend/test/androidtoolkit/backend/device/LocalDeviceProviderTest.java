package androidtoolkit.backend.device;

import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;
import androidtoolkit.app.DeviceMessageResult;
import androidtoolkit.app.ScreenshotCaptureResponse;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.UninstallAppResult;
import androidtoolkit.app.WifiDebugResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LocalDeviceProvider}.
 * Verifies that each method delegates to the correct manager and that
 * adb failures are handled gracefully.
 *
 * Validates: Requirements 6.1, 6.3, 6.5
 */
class LocalDeviceProviderTest {

    private DeviceCatalog deviceCatalog;
    private DeviceActionManager deviceActionManager;
    private ScreenshotManager screenshotManager;
    private LocalDeviceProvider provider;

    @BeforeEach
    void setUp() {
        deviceCatalog = mock(DeviceCatalog.class);
        deviceActionManager = mock(DeviceActionManager.class);
        screenshotManager = mock(ScreenshotManager.class);
        provider = new LocalDeviceProvider(deviceCatalog, deviceActionManager, screenshotManager);
    }

    // --- discoverDevices tests ---

    @Test
    void discoverDevices_delegatesToDeviceCatalog() {
        DeviceDiscoveryResult expected = new DeviceDiscoveryResult(
                List.of("serial-1", "serial-2"), new ArrayList<>());
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class))).thenReturn(expected);

        DeviceDiscoveryResult result = provider.discoverDevices(1L);

        assertSame(expected, result);
        verify(deviceCatalog).discoverDevices(any(DeviceDiscoveryRequest.class));
    }

    @Test
    void discoverDevices_returnsEmptyResultWhenRuntimeExceptionThrown() {
        when(deviceCatalog.discoverDevices(any(DeviceDiscoveryRequest.class)))
                .thenThrow(new RuntimeException("adb not found"));

        DeviceDiscoveryResult result = provider.discoverDevices(1L);

        assertNotNull(result);
        assertTrue(result.getSerials().isEmpty());
        assertTrue(result.getDevices().isEmpty());
        assertEquals(0, result.getDeviceCount());
    }

    // --- reboot tests ---

    @Test
    void reboot_delegatesToDeviceActionManager() {
        String serial = "device-123";
        DeviceMessageResult expected = new DeviceMessageResult("Rebooting device");
        when(deviceActionManager.rebootDevice(serial, serial)).thenReturn(expected);

        DeviceMessageResult result = provider.reboot(1L, serial);

        assertSame(expected, result);
        verify(deviceActionManager).rebootDevice(serial, serial);
    }

    // --- uninstall tests ---

    @Test
    void uninstall_delegatesToDeviceActionManager() {
        String serial = "device-456";
        String packageName = "com.example.app";
        UninstallAppResult expected = new UninstallAppResult(true, "Uninstalled");
        when(deviceActionManager.uninstallApp(serial, serial, packageName)).thenReturn(expected);

        UninstallAppResult result = provider.uninstall(1L, serial, packageName);

        assertSame(expected, result);
        verify(deviceActionManager).uninstallApp(serial, serial, packageName);
    }

    // --- screenshot tests ---

    @Test
    void screenshot_delegatesToScreenshotManager() {
        String serial = "device-789";
        ScreenshotCaptureResponse expected = new ScreenshotCaptureResponse("/screenshots", "Captured");
        when(screenshotManager.captureScreenshot(serial, serial)).thenReturn(expected);

        ScreenshotCaptureResponse result = provider.screenshot(1L, serial);

        assertSame(expected, result);
        verify(screenshotManager).captureScreenshot(serial, serial);
    }

    // --- toggleWifiDebug tests ---

    @Test
    void toggleWifiDebug_delegatesToDeviceActionManager() {
        String serial = "device-wifi";
        String ipAddress = "192.168.1.100";
        boolean wifiDebugSession = true;
        boolean hasWifiIp = true;
        WifiDebugResult expected = new WifiDebugResult(false, true, "Connected", "Disconnect");
        when(deviceActionManager.toggleWifiDebugging(serial, serial, ipAddress, wifiDebugSession, hasWifiIp))
                .thenReturn(expected);

        WifiDebugResult result = provider.toggleWifiDebug(1L, serial, ipAddress, wifiDebugSession, hasWifiIp);

        assertSame(expected, result);
        verify(deviceActionManager).toggleWifiDebugging(serial, serial, ipAddress, wifiDebugSession, hasWifiIp);
    }

    // --- enableFirebaseDebug tests ---

    @Test
    void enableFirebaseDebug_delegatesToDeviceActionManager() {
        String serial = "device-firebase";
        String packageName = "com.example.debug";
        DeviceMessageResult expected = new DeviceMessageResult("Firebase debug enabled");
        when(deviceActionManager.enableFirebaseDebugging(serial, serial, packageName)).thenReturn(expected);

        DeviceMessageResult result = provider.enableFirebaseDebug(1L, serial, packageName);

        assertSame(expected, result);
        verify(deviceActionManager).enableFirebaseDebugging(serial, serial, packageName);
    }
}
