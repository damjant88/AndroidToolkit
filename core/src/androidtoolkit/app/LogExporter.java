package androidtoolkit.app;

import androidtoolkit.service.DeviceActionService;

public class LogExporter {

    private final DeviceActionService deviceActionService;

    public LogExporter(DeviceActionService deviceActionService) {
        this.deviceActionService = deviceActionService;
    }

    public LogExportResult exportDeviceLogs(LogExportRequest request) {
        return new LogExportResult(deviceActionService.saveLogs(request.getSerial(), request.getTargetFolder()));
    }
}
