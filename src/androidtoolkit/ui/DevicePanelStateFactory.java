package androidtoolkit.ui;

import androidtoolkit.domain.DeviceInfo;

import javax.swing.*;
import java.util.Set;

public class DevicePanelStateFactory {

    private static final Set<String> TMO_PACKAGES = Set.of(
            "com.smithmicro.tmobile.familymode.test",
            "com.tmobile.familycontrols"
    );
    private static final Set<String> DISH_PACKAGES = Set.of(
            "com.smithmicro.safepath.dish.test",
            "com.smithmicro.safepath.dish.kid.test"
    );
    private static final Set<String> PRODUCT_PACKAGES = Set.of(
            "com.smithmicro.safepath.family",
            "com.smithmicro.safepath.family.child"
    );
    private static final Set<String> ATT_IAP_PACKAGES = Set.of("com.smithmicro.att.securefamily");
    private static final Set<String> ATT_EAP_PACKAGES = Set.of("com.wavemarket.waplauncher");
    private static final Set<String> ATT_COMPANION_PACKAGES = Set.of("com.att.securefamilycompanion");
    private static final Set<String> SPRINT_PACKAGES = Set.of(
            "com.smithmicro.sprint.safeandfound.test",
            "com.sprint.safefound"
    );
    private static final Set<String> ORANGE_PACKAGES = Set.of(
            "com.smithmicro.orangespain.test",
            "com.orange.es.TuYo"
    );

    public DevicePanelState create(DeviceInfo deviceInfo, Icons icons) {
        String packageName = deviceInfo.getSafePathPackage();
        String wifiButtonText = deviceInfo.isWifiDebugSession() ? "Disable WiFi" : "WiFi Debug";

        if (TMO_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_tmo, "FamilyMode", wifiButtonText);
        }
        if (DISH_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_dish, "Dish", wifiButtonText);
        }
        if (PRODUCT_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_product, "SPFamily", wifiButtonText);
        }
        if (ATT_IAP_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_att, "SF IAP", wifiButtonText);
        }
        if (ATT_EAP_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_att, "SF EAP", wifiButtonText);
        }
        if (ATT_COMPANION_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_att, "SF Companion", wifiButtonText);
        }
        if (SPRINT_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_sprint, "Safe&Found", wifiButtonText);
        }
        if (ORANGE_PACKAGES.contains(packageName)) {
            return installedState(icons.logo_orange, "TuYo", wifiButtonText);
        }

        return new DevicePanelState(
                icons.notInstalled,
                "Not Installed",
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                true,
                wifiButtonText
        );
    }

    private DevicePanelState installedState(ImageIcon icon, String labelText, String wifiButtonText) {
        return new DevicePanelState(
                icon,
                labelText,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                wifiButtonText
        );
    }
}
