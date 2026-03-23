package androidtoolkit.ui;

public class MyFrameStateFactory {

    public MyFrameState create(int numberOfDevices, boolean hasSelectedBuild, boolean hasInstallableDevice, boolean hasInstalledDevice) {
        return new MyFrameState(
                hasSelectedBuild && hasInstallableDevice,
                hasInstalledDevice,
                numberOfDevices * 210 + 230
        );
    }
}
