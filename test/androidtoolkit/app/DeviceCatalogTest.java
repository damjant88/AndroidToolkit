package androidtoolkit.app;

import androidtoolkit.domain.DeviceInfo;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.service.DeviceInfoService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeviceCatalogTest {

    @Test
    void discoverDevicesBuildsIndexedConnectedDevicesFromGatewaySerials() {
        DeviceGateway deviceGateway = mock(DeviceGateway.class);
        DeviceInfoService deviceInfoService = mock(DeviceInfoService.class);
        DeviceCatalog deviceCatalog = new DeviceCatalog(deviceGateway, deviceInfoService);

        ArrayList<String> serials = new ArrayList<>(List.of("SERIAL-1", "SERIAL-2"));
        DeviceInfo firstInfo = new DeviceInfo("SERIAL-1", "Google", "Pixel 8", "14", "", "", "", "pkg.one", true, "101");
        DeviceInfo secondInfo = new DeviceInfo("SERIAL-2", "Samsung", "S24", "14", "", "", "", "pkg.two", false, "");

        when(deviceGateway.getConnectedDevices()).thenReturn(serials);
        when(deviceInfoService.load("SERIAL-1")).thenReturn(firstInfo);
        when(deviceInfoService.load("SERIAL-2")).thenReturn(secondInfo);

        DeviceDiscoveryResult result = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());

        assertEquals(serials, result.getSerials());
        assertEquals(2, result.getDeviceCount());

        List<ConnectedDevice> devices = result.getDevices();
        assertEquals(2, devices.size());

        ConnectedDevice firstDevice = devices.get(0);
        assertEquals(0, firstDevice.getIndex());
        assertEquals("SERIAL-1", firstDevice.getSerial());
        assertEquals("Device1", firstDevice.getDeviceName());
        assertSame(firstInfo, firstDevice.getDeviceInfo());

        ConnectedDevice secondDevice = devices.get(1);
        assertEquals(1, secondDevice.getIndex());
        assertEquals("SERIAL-2", secondDevice.getSerial());
        assertEquals("Device2", secondDevice.getDeviceName());
        assertSame(secondInfo, secondDevice.getDeviceInfo());

        assertTrue(result.getDevices().stream().allMatch(device -> device.getDeviceInfo() != null));
    }
}
