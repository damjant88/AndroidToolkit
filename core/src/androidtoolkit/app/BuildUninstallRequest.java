package androidtoolkit.app;

import java.util.List;

public class BuildUninstallRequest {

    private final List<DeviceTarget> deviceTargets;
    private final String buildName;

    public BuildUninstallRequest(List<DeviceTarget> deviceTargets, String buildName) {
        this.deviceTargets = deviceTargets;
        this.buildName = buildName;
    }

    public List<DeviceTarget> getDeviceTargets() {
        return deviceTargets;
    }

    public String getBuildName() {
        return buildName;
    }
}
