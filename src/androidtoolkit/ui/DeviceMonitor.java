package androidtoolkit.ui;

import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.DeviceDiscoveryRequest;
import androidtoolkit.app.DeviceDiscoveryResult;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

public class DeviceMonitor {

    private final DeviceCatalog deviceCatalog;
    private final Supplier<ArrayList<String>> currentSerialsSupplier;
    private final DeviceMonitorUi deviceMonitorUi;
    private final long pollIntervalMs;

    public DeviceMonitor(
            DeviceCatalog deviceCatalog,
            Supplier<ArrayList<String>> currentSerialsSupplier,
            DeviceMonitorUi deviceMonitorUi,
            long pollIntervalMs
    ) {
        this.deviceCatalog = Objects.requireNonNull(deviceCatalog);
        this.currentSerialsSupplier = Objects.requireNonNull(currentSerialsSupplier);
        this.deviceMonitorUi = Objects.requireNonNull(deviceMonitorUi);
        this.pollIntervalMs = pollIntervalMs;
    }

    public void start() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DeviceDiscoveryResult discoveryResult = deviceCatalog.discoverDevices(new DeviceDiscoveryRequest());
                    if (!discoveryResult.getSerials().equals(currentSerialsSupplier.get())) {
                        deviceMonitorUi.onDeviceListChanged(discoveryResult);
                    }
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.start();
    }

    public interface DeviceMonitorUi {
        void onDeviceListChanged(DeviceDiscoveryResult discoveryResult);
    }
}
