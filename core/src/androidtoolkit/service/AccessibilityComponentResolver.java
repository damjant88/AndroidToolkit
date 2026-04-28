package androidtoolkit.service;

/**
 * Shared utility for building accessibility service component names.
 * Used by both AdbDeviceService and DevicePermissionService.
 */
class AccessibilityComponentResolver {

    static String toComponent(String appPackage, String serviceClassName) {
        if (serviceClassName == null || serviceClassName.isBlank()) {
            return "";
        }
        if (serviceClassName.contains("/")) {
            return serviceClassName;
        }
        if (serviceClassName.startsWith(".")) {
            return appPackage + "/" + appPackage + serviceClassName;
        }
        return appPackage + "/" + serviceClassName;
    }
}
