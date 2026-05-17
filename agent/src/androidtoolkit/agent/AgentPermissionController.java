package androidtoolkit.agent;

import androidtoolkit.app.PermissionCatalog;
import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionDialogState;
import androidtoolkit.app.PermissionManager;
import androidtoolkit.app.PermissionUpdateResponse;
import androidtoolkit.service.AdbDeviceService;
import androidtoolkit.service.DevicePermissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Permissions endpoint on the agent — uses the core module's PermissionManager
 * with a local AdbDeviceService to check and apply permissions via adb.
 */
@RestController
@RequestMapping("/api/agent/devices")
@CrossOrigin(origins = "*")
public class AgentPermissionController {

    private final PermissionManager permissionManager;

    public AgentPermissionController() {
        // Create the permission stack using local adb
        androidtoolkit.service.CommandExecutor commandExecutor = new androidtoolkit.service.CommandExecutor();
        androidtoolkit.service.PackageClassifier packageClassifier = new androidtoolkit.service.PackageClassifier();
        androidtoolkit.service.StoragePaths storagePaths = new androidtoolkit.service.StoragePaths();
        AdbDeviceService adbService = new AdbDeviceService(commandExecutor, packageClassifier, storagePaths);
        PermissionCatalog catalog = new PermissionCatalog();
        DevicePermissionService permissionService = new DevicePermissionService(adbService);
        this.permissionManager = new PermissionManager(catalog, permissionService);
    }

    @GetMapping("/{serial}/permissions")
    public PermissionDialogState getPermissions(@PathVariable String serial, @RequestParam String packageName) {
        return permissionManager.loadDialogState(serial, packageName);
    }

    @PostMapping("/{serial}/permissions/enable")
    public PermissionUpdateResponse enablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        String packageName = String.valueOf(body.getOrDefault("packageName", ""));
        List<?> rawIds = (List<?>) body.getOrDefault("permissionIds", List.of());
        List<String> permissionIds = rawIds.stream().map(String::valueOf).toList();
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.enablePermissions(serial, packageName, definitions);
    }

    @PostMapping("/{serial}/permissions/disable")
    public PermissionUpdateResponse disablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        String packageName = String.valueOf(body.getOrDefault("packageName", ""));
        List<?> rawIds = (List<?>) body.getOrDefault("permissionIds", List.of());
        List<String> permissionIds = rawIds.stream().map(String::valueOf).toList();
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.disablePermissions(serial, packageName, definitions);
    }
}
