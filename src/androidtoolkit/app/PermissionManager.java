package androidtoolkit.app;

import androidtoolkit.service.DevicePermissionService;
import androidtoolkit.service.PermissionStateSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private final PermissionCatalog permissionCatalog;
    private final DevicePermissionService devicePermissionService;

    public PermissionManager(PermissionCatalog permissionCatalog, DevicePermissionService devicePermissionService) {
        this.permissionCatalog = permissionCatalog;
        this.devicePermissionService = devicePermissionService;
    }

    public PermissionDialogState loadDialogState(String serial, String packageName) {
        List<PermissionDefinition> definitions = permissionCatalog.supportedPermissionsFor(packageName);
        if (definitions.isEmpty()) {
            return new PermissionDialogState(packageName, definitions, List.of(), List.of());
        }

        PermissionStateSnapshot snapshot = devicePermissionService.loadPermissionStates(serial, packageName, definitions);
        return new PermissionDialogState(
                packageName,
                definitions,
                new ArrayList<>(snapshot.getActivePermissionIds()),
                new ArrayList<>(snapshot.getUnavailablePermissionIds())
        );
    }

    public PermissionUpdateResponse enablePermissions(String serial, String packageName, List<PermissionDefinition> selections) {
        PermissionUpdateResult result = devicePermissionService.applyPermissions(serial, packageName, selections);
        return new PermissionUpdateResponse(result, loadDialogState(serial, packageName));
    }

    public PermissionUpdateResponse disablePermissions(String serial, String packageName, List<PermissionDefinition> selections) {
        PermissionUpdateResult result = devicePermissionService.disablePermissions(serial, packageName, selections);
        return new PermissionUpdateResponse(result, loadDialogState(serial, packageName));
    }
}
