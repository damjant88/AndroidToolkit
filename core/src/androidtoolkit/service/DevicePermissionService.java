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
                    case ACCESSIBILITY_SERVICE:
                        if (enable) {
                            deviceGateway.enableAccessibilityService(serial, packageName, selection.getCommandValue());
                        } else {
                            deviceGateway.disableAccessibilityService(serial, packageName, selection.getCommandValue());
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

    public PermissionStateSnapshot loadPermissionStates(String serial, String packageName, List<PermissionDefinition> definitions) {
        Set<String> activePermissionIds = new LinkedHashSet<>();
        Set<String> unavailablePermissionIds = new LinkedHashSet<>();
        String packageDump = deviceGateway.getPackageDump(serial, packageName);
        String deviceIdleWhitelist = null;
        String autoRevokeState = null;

        for (PermissionDefinition definition : definitions) {
            boolean isAvailable = true;
            boolean isActive;
            switch (definition.getCommandType()) {
                case GRANT_PERMISSION:
                    isAvailable = isPermissionRequestDeclared(packageDump, definition.getCommandValue());
                    isActive = isAvailable && isPermissionGranted(packageDump, definition.getCommandValue());
                    break;
                case DEVICE_IDLE_WHITELIST:
                    if (deviceIdleWhitelist == null) {
                        deviceIdleWhitelist = deviceGateway.isInDeviceIdleWhitelist(serial, packageName) ? packageName : "";
                    }
                    isActive = deviceIdleWhitelist.contains(packageName);
                    break;
                case IGNORE_AUTO_REVOKE:
                    if (autoRevokeState == null) {
                        autoRevokeState = deviceGateway.getAutoRevokePermissionsState(serial, packageName);
                    }
                    isActive = autoRevokeState.toLowerCase().contains("ignore");
                    break;
                case ACCESSIBILITY_SERVICE:
                    isAvailable = isAccessibilityServiceDeclared(packageDump, packageName, definition.getCommandValue());
                    isActive = isAvailable && deviceGateway.isAccessibilityServiceEnabled(serial, packageName, definition.getCommandValue());
                    break;
                default:
                    throw new IllegalStateException("Unsupported permission action: " + definition.getCommandType());
            }
            if (!isAvailable) {
                unavailablePermissionIds.add(definition.getId());
                continue;
            }
            if (isActive) {
                activePermissionIds.add(definition.getId());
            }
        }
        return new PermissionStateSnapshot(activePermissionIds, unavailablePermissionIds);
    }

    private boolean isPermissionRequestDeclared(String packageDump, String permission) {
        return packageDump.contains("requested permissions:")
                && packageDump.contains(permission);
    }

    private boolean isPermissionGranted(String packageDump, String permission) {
        return packageDump.contains(permission + ": granted=true")
                || packageDump.contains(permission + " granted=true");
    }

    private boolean isAccessibilityServiceDeclared(String packageDump, String packageName, String serviceClassName) {
        String component = AccessibilityComponentResolver.toComponent(packageName, serviceClassName);
        return packageDump.contains("android.accessibilityservice.AccessibilityService")
                && packageDump.contains(component);
    }

}
