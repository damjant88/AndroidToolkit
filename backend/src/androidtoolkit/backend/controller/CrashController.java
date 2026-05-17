package androidtoolkit.backend.controller;

import androidtoolkit.backend.crash.*;
import androidtoolkit.backend.service.ObjectStorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.LocalDate;

/**
 * REST controller for crash event history, detail, and acknowledgement.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/crashes")
public class CrashController {

    private final CrashHistoryService crashHistoryService;
    private final ObjectStorageService objectStorageService;

    public CrashController(CrashHistoryService crashHistoryService,
                           ObjectStorageService objectStorageService) {
        this.crashHistoryService = crashHistoryService;
        this.objectStorageService = objectStorageService;
    }

    /**
     * GET /api/projects/{projectId}/crashes
     * Returns paginated crash history with optional filters.
     */
    @GetMapping
    public Page<CrashEventEntity> getCrashHistory(
            @PathVariable Long projectId,
            @RequestParam(required = false) String deviceSerial,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) SeverityLevel severity,
            @RequestParam(required = false) String crashType,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CrashEventFilter filter = new CrashEventFilter(
                deviceSerial, fromDate, toDate, severity, crashType, acknowledged);
        Pageable pageable = PageRequest.of(page, size);

        return crashHistoryService.findByProject(projectId, filter, pageable);
    }

    /**
     * GET /api/projects/{projectId}/crashes/{crashId}
     * Returns a single crash event detail.
     */
    @GetMapping("/{crashId}")
    public ResponseEntity<CrashEventEntity> getCrashDetail(
            @PathVariable Long projectId,
            @PathVariable Long crashId) {

        return crashHistoryService.findById(crashId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/projects/{projectId}/crashes/{crashId}/log
     * Downloads the crash log file from Object Storage.
     */
    @GetMapping("/{crashId}/log")
    public ResponseEntity<InputStreamResource> downloadCrashLog(
            @PathVariable Long projectId,
            @PathVariable Long crashId) {

        CrashEventEntity entity = crashHistoryService.findById(crashId)
                .orElse(null);

        if (entity == null || entity.getCrashLogPath() == null) {
            return ResponseEntity.notFound().build();
        }

        InputStream stream = objectStorageService.download(entity.getCrashLogPath());
        InputStreamResource resource = new InputStreamResource(stream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"crash-" + crashId + ".log\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    /**
     * POST /api/projects/{projectId}/crashes/{crashId}/acknowledge
     * Acknowledges a crash event.
     */
    @PostMapping("/{crashId}/acknowledge")
    public ResponseEntity<CrashEventEntity> acknowledgeCrash(
            @PathVariable Long projectId,
            @PathVariable Long crashId) {

        CrashEventEntity entity = crashHistoryService.acknowledge(crashId);
        return ResponseEntity.ok(entity);
    }
}
