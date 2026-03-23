package androidtoolkit.app;

import androidtoolkit.service.DeviceActionService;

public class LogExporter {

    private final DeviceActionService deviceActionService;

    public LogExporter(DeviceActionService deviceActionService) {
        this.deviceActionService = deviceActionService;
    }

    public String exportDeviceLogs(String serial, String targetFolder) {
        return deviceActionService.saveLogs(serial, targetFolder);
    }
}
