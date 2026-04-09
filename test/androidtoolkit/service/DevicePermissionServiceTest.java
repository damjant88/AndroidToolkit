package androidtoolkit.service;

import androidtoolkit.app.PermissionCommandType;
import androidtoolkit.app.PermissionDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevicePermissionServiceTest {

    @Test
    void loadPermissionStatesReusesSharedPackageAndAppOpsData() {
        DeviceGateway deviceGateway = mock(DeviceGateway.class);
        DevicePermissionService service = new DevicePermissionService(deviceGateway);

        List<PermissionDefinition> definitions = List.of(
                new PermissionDefinition(
                        "fine_location",
                        "ACCESS_FINE_LOCATION",
                        PermissionCommandType.GRANT_PERMISSION,
                        "android.permission.ACCESS_FINE_LOCATION"
                ),
                new PermissionDefinition(
                        "notifications",
                        "POST_NOTIFICATIONS",
                        PermissionCommandType.GRANT_PERMISSION,
                        "android.permission.POST_NOTIFICATIONS"
                ),
                new PermissionDefinition(
                        "battery_optimization",
                        "Disable Battery Optimization",
                        PermissionCommandType.DEVICE_IDLE_WHITELIST,
                        ""
                ),
                new PermissionDefinition(
                        "auto_reset",
                        "Prevent Auto Reset Permissions",
                        PermissionCommandType.IGNORE_AUTO_REVOKE,
                        ""
                )
        );

        String packageDump = String.join("\n",
                "requested permissions:",
                "  android.permission.ACCESS_FINE_LOCATION",
                "runtime permissions:",
                "  android.permission.ACCESS_FINE_LOCATION: granted=true");

        when(deviceGateway.getPackageDump("SERIAL-1", "pkg.test")).thenReturn(packageDump);
        when(deviceGateway.isInDeviceIdleWhitelist("SERIAL-1", "pkg.test")).thenReturn(true);
        when(deviceGateway.getAutoRevokePermissionsState("SERIAL-1", "pkg.test"))
                .thenReturn("AUTO_REVOKE_PERMISSIONS_IF_UNUSED: ignore");

        PermissionStateSnapshot snapshot = service.loadPermissionStates("SERIAL-1", "pkg.test", definitions);

        assertEquals(List.of("fine_location", "battery_optimization", "auto_reset"),
                new ArrayList<>(snapshot.getActivePermissionIds()));
        assertEquals(List.of("notifications"),
                new ArrayList<>(snapshot.getUnavailablePermissionIds()));

        verify(deviceGateway, times(1)).getPackageDump("SERIAL-1", "pkg.test");
        verify(deviceGateway, times(1)).isInDeviceIdleWhitelist("SERIAL-1", "pkg.test");
        verify(deviceGateway, times(1)).getAutoRevokePermissionsState("SERIAL-1", "pkg.test");
    }
}
