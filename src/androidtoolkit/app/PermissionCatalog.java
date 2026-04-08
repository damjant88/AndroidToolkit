package androidtoolkit.app;

import java.util.ArrayList;
import java.util.List;

public class PermissionCatalog {

    private static final String CCI_PACKAGE = "com.smithmicro.cci.test";
    private static final String SAFEPATH_FAMILY_PACKAGE = "com.smithmicro.safepath.family";

    public List<PermissionDefinition> supportedPermissionsFor(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return new ArrayList<>();
        }
        if (CCI_PACKAGE.equals(packageName)) {
            return cciPermissions();
        }
        if (SAFEPATH_FAMILY_PACKAGE.equals(packageName)) {
            return safePathFamilyPermissions();
        }
        return new ArrayList<>();
    }

    private List<PermissionDefinition> cciPermissions() {
        List<PermissionDefinition> definitions = new ArrayList<>();
        definitions.add(new PermissionDefinition(
                "fine_location",
                "ACCESS_FINE_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_FINE_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "coarse_location",
                "ACCESS_COARSE_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_COARSE_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "background_location",
                "ACCESS_BACKGROUND_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_BACKGROUND_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "read_phone_state",
                "READ_PHONE_STATE",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.READ_PHONE_STATE"
        ));
        definitions.add(new PermissionDefinition(
                "read_phone_numbers",
                "READ_PHONE_NUMBERS",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.READ_PHONE_NUMBERS"
        ));
        definitions.add(new PermissionDefinition(
                "post_notifications",
                "POST_NOTIFICATIONS",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.POST_NOTIFICATIONS"
        ));
        definitions.add(new PermissionDefinition(
                "deviceidle_whitelist",
                "Disable Battery Optimization",
                PermissionCommandType.DEVICE_IDLE_WHITELIST,
                ""
        ));
        definitions.add(new PermissionDefinition(
                "auto_revoke_ignore",
                "Prevent Auto Reset Permissions",
                PermissionCommandType.IGNORE_AUTO_REVOKE,
                ""
        ));
        return definitions;
    }

    private List<PermissionDefinition> safePathFamilyPermissions() {
        List<PermissionDefinition> definitions = new ArrayList<>();
        definitions.add(new PermissionDefinition(
                "fine_location",
                "ACCESS_FINE_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_FINE_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "coarse_location",
                "ACCESS_COARSE_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_COARSE_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "background_location",
                "ACCESS_BACKGROUND_LOCATION",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.ACCESS_BACKGROUND_LOCATION"
        ));
        definitions.add(new PermissionDefinition(
                "post_notifications",
                "POST_NOTIFICATIONS",
                PermissionCommandType.GRANT_PERMISSION,
                "android.permission.POST_NOTIFICATIONS"
        ));
        definitions.add(new PermissionDefinition(
                "deviceidle_whitelist",
                "Disable Battery Optimization",
                PermissionCommandType.DEVICE_IDLE_WHITELIST,
                ""
        ));
        return definitions;
    }
}
