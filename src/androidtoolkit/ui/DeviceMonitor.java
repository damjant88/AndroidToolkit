package androidtoolkit.ui;

import androidtoolkit.app.DeviceCatalog;

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
                    ArrayList<String> latestSerials = deviceCatalog.connectedSerials();
                    if (!latestSerials.equals(currentSerialsSupplier.get())) {
                        deviceMonitorUi.onDeviceListChanged(latestSerials);
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
        void onDeviceListChanged(ArrayList<String> latestSerials);
    }
}
