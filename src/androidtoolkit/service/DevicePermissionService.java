package androidtoolkit.service;

import androidtoolkit.app.PermissionCommandType;
import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionUpdateResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DevicePermissionService {

    private final DeviceGateway deviceGateway;

    public DevicePermissionService(DeviceGateway deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    public PermissionUpdateResult applyPermissions(String serial, String packageName, List<PermissionDefinition> selections) {
        return updatePermissions(serial, packageName, selections, true);
    }

    public PermissionUpdateResult disablePermissions(String serial, String packageName, List<PermissionDefinition> selections) {
        return updatePermissions(serial, packageName, selections, false);
    }

    private PermissionUpdateResult updatePermissions(String serial, String packageName, List<PermissionDefinition> selections, boolean enable) {
        List<String> failures = new ArrayList<>();
        int appliedCount = 0;

        for (PermissionDefinition selection : selections) {
            try {
                switch (selection.getCommandType()) {
                    case GRANT_PERMISSION:
                        if (enable) {
                            deviceGateway.grantPermission(serial, packageName, selection.getCommandValue());
                        } else {
                            deviceGateway.revokePermission(serial, packageName, selection.getCommandValue());
                        }
                        break;
                    case DEVICE_IDLE_WHITELIST:
                        if (enable) {
                            deviceGateway.addToDeviceIdleWhitelist(serial, packageName);
                        } else {
                            deviceGateway.removeFromDeviceIdleWhitelist(serial, packageName);
                        }
                        break;
                    case IGNORE_AUTO_REVOKE:
                        if (enable) {
                            deviceGateway.ignoreAutoRevokePermissions(serial, packageName);
                        } else {
                            deviceGateway.resetAutoRevokePermissions(serial, packageName);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unsupported permission action: " + selection.getCommandType());
                }
                appliedCount++;
            } catch (RuntimeException ex) {
                failures.add(selection.getLabel());
            }
        }

        return new PermissionUpdateResult(appliedCount, selections.size(), enable ? "enabled" : "disabled", failures);
    }

    public Set<String> loadActivePermissionIds(String serial, String packageName, List<PermissionDefinition> definitions) {
        Set<String> activePermissionIds = new LinkedHashSet<>();
        for (PermissionDefinition definition : definitions) {
            boolean isActive;
            switch (definition.getCommandType()) {
                case GRANT_PERMISSION:
                    isActive = deviceGateway.isPermissionGranted(serial, packageName, definition.getCommandValue());
                    break;
                case DEVICE_IDLE_WHITELIST:
                    isActive = deviceGateway.isInDeviceIdleWhitelist(serial, packageName);
                    break;
                case IGNORE_AUTO_REVOKE:
                    isActive = deviceGateway.isAutoRevokeIgnored(serial, packageName);
                    break;
                default:
                    throw new IllegalStateException("Unsupported permission action: " + definition.getCommandType());
            }
            if (isActive) {
                activePermissionIds.add(definition.getId());
            }
        }
        return activePermissionIds;
    }
}
