package androidtoolkit.app;

import androidtoolkit.service.AdbDeviceService;
import androidtoolkit.service.BuildSelectionStore;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceActionService;
import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.PackageClassifier;
import androidtoolkit.service.ScreenRecordingService;
import androidtoolkit.service.StoragePaths;

public class AppServices {

    private final StoragePaths storagePaths;
    private final CommandExecutor commandExecutor;
    private final PackageClassifier packageClassifier;
    private final AdbDeviceService adbDeviceService;
    private final BuildSelectionStore buildSelectionStore;
    private final DeviceActionService deviceActionService;
    private final DeviceInfoService deviceInfoService;
    private final ScreenRecordingService screenRecordingService;

    public AppServices() {
        this.storagePaths = new StoragePaths();
        this.commandExecutor = new CommandExecutor();
        this.packageClassifier = new PackageClassifier();
        this.adbDeviceService = new AdbDeviceService(commandExecutor, packageClassifier, storagePaths);
        this.buildSelectionStore = new BuildSelectionStore(storagePaths);
        this.deviceActionService = new DeviceActionService(adbDeviceService, storagePaths);
        this.deviceInfoService = new DeviceInfoService(adbDeviceService, commandExecutor);
        this.screenRecordingService = new ScreenRecordingService(commandExecutor, adbDeviceService, storagePaths);
    }

    public StoragePaths storagePaths() {
        return storagePaths;
    }

    public CommandExecutor commandExecutor() {
        return commandExecutor;
    }

    public AdbDeviceService adbDeviceService() {
        return adbDeviceService;
    }

    public BuildSelectionStore buildSelectionStore() {
        return buildSelectionStore;
    }

    public DeviceActionService deviceActionService() {
        return deviceActionService;
    }

    public DeviceInfoService deviceInfoService() {
        return deviceInfoService;
    }

    public ScreenRecordingService screenRecordingService() {
        return screenRecordingService;
    }
}
