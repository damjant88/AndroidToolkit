package androidtoolkit.app;

import androidtoolkit.service.AdbDeviceService;
import androidtoolkit.service.BuildSelectionStore;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.service.DeviceActionService;
import androidtoolkit.service.DeviceGateway;
import androidtoolkit.service.DeviceInfoService;
import androidtoolkit.service.DevicePermissionService;
import androidtoolkit.service.HostToolsGateway;
import androidtoolkit.service.LocalHostToolsGateway;
import androidtoolkit.service.LocalStorageService;
import androidtoolkit.service.PackageClassifier;
import androidtoolkit.service.ScreenRecordingService;
import androidtoolkit.service.StorageService;
import androidtoolkit.service.StoragePaths;

public class AppServices {

    private final StoragePaths storagePaths;
    private final StorageService storageService;
    private final HostToolsGateway hostToolsGateway;
    private final CommandExecutor commandExecutor;
    private final PackageClassifier packageClassifier;
    private final DeviceGateway deviceGateway;
    private final BuildSelectionStore buildSelectionStore;
    private final DeviceActionService deviceActionService;
    private final DeviceInfoService deviceInfoService;
    private final ScreenRecordingService screenRecordingService;
    private final DevicePermissionService devicePermissionService;
    private final BuildInstaller buildInstaller;
    private final DeviceCatalog deviceCatalog;
    private final LogExporter logExporter;
    private final DeviceOperations deviceOperations;
    private final PermissionCatalog permissionCatalog;

    public AppServices() {
        this.storagePaths = new StoragePaths();
        this.storageService = new LocalStorageService(storagePaths);
        this.hostToolsGateway = new LocalHostToolsGateway();
        this.commandExecutor = new CommandExecutor();
        this.packageClassifier = new PackageClassifier();
        this.deviceGateway = new AdbDeviceService(commandExecutor, packageClassifier, storagePaths);
        this.buildSelectionStore = new BuildSelectionStore(storageService);
        this.deviceActionService = new DeviceActionService(deviceGateway, storageService, hostToolsGateway);
        this.deviceInfoService = new DeviceInfoService(deviceGateway, commandExecutor);
        this.screenRecordingService = new ScreenRecordingService(commandExecutor, deviceGateway, storageService, hostToolsGateway);
        this.devicePermissionService = new DevicePermissionService(deviceGateway);
        this.buildInstaller = new BuildInstaller(deviceGateway, commandExecutor);
        this.deviceCatalog = new DeviceCatalog(deviceGateway, deviceInfoService);
        this.logExporter = new LogExporter(deviceActionService);
        this.deviceOperations = new DeviceOperations(deviceActionService, screenRecordingService);
        this.permissionCatalog = new PermissionCatalog();
    }

    public StoragePaths storagePaths() {
        return storagePaths;
    }

    public StorageService storageService() {
        return storageService;
    }

    public HostToolsGateway hostToolsGateway() {
        return hostToolsGateway;
    }

    public CommandExecutor commandExecutor() {
        return commandExecutor;
    }

    public DeviceGateway deviceGateway() {
        return deviceGateway;
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

    public DevicePermissionService devicePermissionService() {
        return devicePermissionService;
    }

    public BuildInstaller buildInstaller() {
        return buildInstaller;
    }

    public DeviceCatalog deviceCatalog() {
        return deviceCatalog;
    }

    public LogExporter logExporter() {
        return logExporter;
    }

    public DeviceOperations deviceOperations() {
        return deviceOperations;
    }

    public PermissionCatalog permissionCatalog() {
        return permissionCatalog;
    }
}
