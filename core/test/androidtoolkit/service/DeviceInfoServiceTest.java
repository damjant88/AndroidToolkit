package androidtoolkit.service;

import androidtoolkit.domain.DeviceInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceInfoServiceTest {

    @Test
    void loadUsesDetectedPackageToDetermineInstalledState() {
        DeviceGateway deviceGateway = mock(DeviceGateway.class);
        CommandExecutor commandExecutor = mock(CommandExecutor.class);
        DeviceInfoService service = new DeviceInfoService(deviceGateway, commandExecutor);

        when(deviceGateway.getDeviceManufacturer("SERIAL-1")).thenReturn("Google");
        when(deviceGateway.getDeviceModel("SERIAL-1")).thenReturn("Pixel");
        when(deviceGateway.getDeviceOSVersion("SERIAL-1")).thenReturn("14");
        when(deviceGateway.getSafePathPackage("SERIAL-1")).thenReturn("pkg.test");
        when(deviceGateway.getWlanIp("SERIAL-1")).thenReturn("192.168.1.20");
        when(deviceGateway.getMobileIp("SERIAL-1")).thenReturn("10.0.0.2");
        when(commandExecutor.runCommand("adb -s SERIAL-1 shell pidof -s pkg.test")).thenReturn("321");

        DeviceInfo info = service.load("SERIAL-1");

        assertTrue(info.isAppInstalled());
        assertEquals("pkg.test", info.getSafePathPackage());
        assertEquals("321", info.getPid());
        assertEquals("192.168.1.20", info.getIpAddress());
        verify(deviceGateway).getSafePathPackage("SERIAL-1");
    }

    @Test
    void loadLeavesPidBlankWhenNoSupportedPackageIsInstalled() {
        DeviceGateway deviceGateway = mock(DeviceGateway.class);
        CommandExecutor commandExecutor = mock(CommandExecutor.class);
        DeviceInfoService service = new DeviceInfoService(deviceGateway, commandExecutor);

        when(deviceGateway.getDeviceManufacturer("SERIAL-2")).thenReturn("Samsung");
        when(deviceGateway.getDeviceModel("SERIAL-2")).thenReturn("S24");
        when(deviceGateway.getDeviceOSVersion("SERIAL-2")).thenReturn("14");
        when(deviceGateway.getSafePathPackage("SERIAL-2")).thenReturn("");
        when(deviceGateway.getWlanIp("SERIAL-2")).thenReturn("");
        when(deviceGateway.getMobileIp("SERIAL-2")).thenReturn("10.0.0.3");

        DeviceInfo info = service.load("SERIAL-2");

        assertFalse(info.isAppInstalled());
        assertEquals("", info.getPid());
        assertEquals("10.0.0.3", info.getIpAddress());
    }
}
