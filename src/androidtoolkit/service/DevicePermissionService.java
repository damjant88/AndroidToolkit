package androidtoolkit.service;

import androidtoolkit.app.PermissionCommandType;
import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionUpdateResult;

import java.util.ArrayList;
import java.util.List;

public class DevicePermissionService {

    private final DeviceGateway deviceGateway;

    public DevicePermissionService(DeviceGateway deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    public PermissionUpdateResult applyPermissions(String serial, String packageName, List<PermissionDefinition> selections) {
        List<String> failures = new ArrayList<>();
        int appliedCount = 0;

        for (PermissionDefinition selection : selections) {
            try {
                switch (selection.getCommandType()) {
                    case GRANT_PERMISSION:
                        deviceGateway.grantPermission(serial, packageName, selection.getCommandValue());
                        break;
                    case DEVICE_IDLE_WHITELIST:
                        deviceGateway.addToDeviceIdleWhitelist(serial, packageName);
                        break;
                    case IGNORE_AUTO_REVOKE:
                        deviceGateway.ignoreAutoRevokePermissions(serial, packageName);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported permission action: " + selection.getCommandType());
                }
                appliedCount++;
            } catch (RuntimeException ex) {
                failures.add(selection.getLabel());
            }
        }

        return new PermissionUpdateResult(appliedCount, selections.size(), failures);
    }
}
