package androidtoolkit.backend.controller;

import androidtoolkit.backend.service.MigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {

    private final MigrationService migrationService;

    public MigrationController(MigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "Migrated Tenant") String tenantName
    ) throws Exception {
        var result = migrationService.importStandaloneArchive(file.getInputStream(), tenantName);

        HttpStatus status = result.conflicts().isEmpty() ? HttpStatus.OK : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(Map.of(
                "projectsImported", result.projectsImported(),
                "usersImported", result.usersImported(),
                "logsImported", result.logsImported(),
                "conflicts", result.conflicts()
        ));
    }
}
