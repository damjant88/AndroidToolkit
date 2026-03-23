package androidtoolkit.app;

import java.util.List;

public class BuildInstallRequest {

    private final List<DeviceTarget> deviceTargets;
    private final String buildPath;
    private final String buildName;

    public BuildInstallRequest(List<DeviceTarget> deviceTargets, String buildPath, String buildName) {
        this.deviceTargets = deviceTargets;
        this.buildPath = buildPath;
        this.buildName = buildName;
    }

    public List<DeviceTarget> getDeviceTargets() {
        return deviceTargets;
    }

    public String getBuildPath() {
        return buildPath;
    }

    public String getBuildName() {
        return buildName;
    }
}
