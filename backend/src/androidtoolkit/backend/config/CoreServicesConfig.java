package androidtoolkit.backend.config;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.DeviceActionManager;
import androidtoolkit.app.DeviceCatalog;
import androidtoolkit.app.PermissionManager;
import androidtoolkit.app.RecordingManager;
import androidtoolkit.app.ScreenshotManager;
import androidtoolkit.app.LogExportManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the existing core AppServices into Spring's dependency injection.
 * Each bean is the same instance the desktop app uses — just exposed to Spring.
 */
@Configuration
public class CoreServicesConfig {

    private final AppServices appServices = new AppServices();

    @Bean
    public AppServices appServices() {
        return appServices;
    }

    @Bean
    public DeviceCatalog deviceCatalog() {
        return appServices.deviceCatalog();
    }

    @Bean
    public DeviceActionManager deviceActionManager() {
        return appServices.deviceActionManager();
    }

    @Bean
    public PermissionManager permissionManager() {
        return appServices.permissionManager();
    }

    @Bean
    public RecordingManager recordingManager() {
        return appServices.recordingManager();
    }

    @Bean
    public ScreenshotManager screenshotManager() {
        return appServices.screenshotManager();
    }

    @Bean
    public LogExportManager logExportManager() {
        return appServices.logExportManager();
    }
}
