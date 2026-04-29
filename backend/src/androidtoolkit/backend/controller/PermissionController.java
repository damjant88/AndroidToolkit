package androidtoolkit.backend.controller;

import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionDialogState;
import androidtoolkit.app.PermissionManager;
import androidtoolkit.app.PermissionUpdateResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static androidtoolkit.backend.validation.InputValidator.validatePackageName;
import static androidtoolkit.backend.validation.InputValidator.validateSerial;

@RestController
@RequestMapping("/api/devices")
public class PermissionController {

    private final PermissionManager permissionManager;

    public PermissionController(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    @GetMapping("/{serial}/permissions")
    public PermissionDialogState getPermissions(@PathVariable String serial, @RequestParam String packageName) {
        validateSerial(serial);
        validatePackageName(packageName);
        return permissionManager.loadDialogState(serial, packageName);
    }

    @PostMapping("/{serial}/permissions/enable")
    public PermissionUpdateResponse enablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        String packageName = (String) body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        List<String> permissionIds = (List<String>) body.getOrDefault("permissionIds", List.of());
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.enablePermissions(serial, packageName, definitions);
    }

    @PostMapping("/{serial}/permissions/disable")
    public PermissionUpdateResponse disablePermissions(@PathVariable String serial, @RequestBody Map<String, Object> body) {
        validateSerial(serial);
        String packageName = (String) body.getOrDefault("packageName", "");
        validatePackageName(packageName);
        List<String> permissionIds = (List<String>) body.getOrDefault("permissionIds", List.of());
        List<PermissionDefinition> definitions = permissionManager.loadDialogState(serial, packageName)
                .getDefinitions().stream()
                .filter(d -> permissionIds.contains(d.getId()))
                .toList();
        return permissionManager.disablePermissions(serial, packageName, definitions);
    }
}
