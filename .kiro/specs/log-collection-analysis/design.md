# Design Document: Log Collection & Analysis

## Overview

This design extends the existing "Pull SP Logs" functionality to add centralized log storage and automated AI-driven analysis. The current flow (`DeviceController.pullLogs` → `LogExportManager.exportDeviceLogs` → `LogExporter` → `DeviceActionService.saveLogs`) saves logs locally. This feature wraps that flow with additional steps: ZIP archiving, upload to shared network storage, metadata persistence, and a scheduled overnight AI analysis job.

The architecture introduces:
1. A **LogCollectionService** that orchestrates local save + ZIP + upload
2. New JPA entities for upload metadata and analysis reports
3. A **LogAnalysisJob** scheduled task using Spring's `@Scheduled` for overnight AI processing
4. New REST endpoints for analysis report retrieval and job monitoring
5. A `sharedLogStoragePath` field on the existing `Project` entity

The design preserves backward compatibility — the existing `LogExportManager` behavior is unchanged; the new service wraps it.

## Architecture

```mermaid
flowchart TD
    A[DeviceController] -->|POST /api/devices/{serial}/pull-logs| B[LogCollectionService]
    B --> C[LogExportManager - existing]
    C --> D[Local file system]
    B --> E[ZipArchiveService]
    E --> F[Shared Network Storage]
    B --> G[LogUploadMetadataRepository]
    G --> H[(Database)]
    
    I[Spring @Scheduled] --> J[LogAnalysisJob]
    J --> K[LogUploadMetadataRepository]
    J --> L[Shared Storage - read ZIPs]
    J --> M[AI Analysis Engine]
    M --> N[AnalysisReportRepository]
    N --> H
    
    O[LogAnalysisController] --> N
    O --> P[AnalysisJobExecutionRepository]
```

### Key Design Decisions

1. **Wrap, don't modify**: The existing `LogExportManager` in the `core` module remains untouched. The new `LogCollectionService` in the `backend` module calls it and adds post-processing.
2. **Async upload**: The shared storage upload runs asynchronously after the local save completes, so the user gets an immediate response.
3. **Graceful degradation**: If shared storage is unreachable, the operation still succeeds (local save is the primary outcome).
4. **Spring @Scheduled for overnight job**: Leverages Spring's built-in scheduling rather than a custom job framework, since the existing `JobService` is designed for short-lived install/uninstall operations with WebSocket progress tracking — not suitable for long-running overnight batch processing.
5. **AI integration via interface**: The AI analysis engine is abstracted behind an interface to allow swapping implementations (OpenAI, local LLM, etc.).
6. **NIO-based shared storage — local-first, remote-ready**: The `SharedStorageService` is intentionally implemented using `java.nio.file` operations (`Path`, `Files.copy`, `Files.createDirectories`, etc.) so it is completely agnostic to whether the underlying path is local or remote.
   - **Initial deployment** uses a local filesystem path for shared storage (e.g., a configurable local directory such as `/opt/androidtoolkit/shared-logs/`).
   - **Transitioning to remote storage** (mounted network drive via NFS/SMB, AWS EFS, or an S3-backed FUSE mount) requires only a configuration change to the project's `sharedLogStoragePath` field — no code changes are needed.
   - Because `java.nio.file.Path` and `Files` operations work transparently over any mounted filesystem, the same service code handles local directories and remote mounts identically.

## Components and Interfaces

### 1. LogCollectionService

```java
@Service
public class LogCollectionService {

    private final LogExportManager logExportManager;
    private final AppServices appServices;
    private final ZipArchiveService zipArchiveService;
    private final SharedStorageService sharedStorageService;
    private final LogUploadMetadataRepository metadataRepository;
    private final ProjectRepository projectRepository;
    private final DeviceGateway deviceGateway;
    private final PackageClassifier packageClassifier;

    /**
     * Pulls logs from device, saves locally, zips, and uploads to shared storage.
     * Returns immediately after local save; upload happens async.
     */
    public LogCollectionResponse collectLogs(String serial, Long projectId);
}
```

### 2. ZipArchiveService

```java
@Service
public class ZipArchiveService {

    /**
     * Creates a ZIP archive from a directory of log files.
     * @param sourceDir the directory containing exported log files
     * @param archiveName the name for the ZIP file (e.g., "logs_14-30-00.zip")
     * @return Path to the created ZIP file
     */
    public Path createArchive(Path sourceDir, String archiveName);
}
```

### 3. SharedStorageService

The `SharedStorageService` is implemented exclusively using `java.nio.file` operations (`Files.copy`, `Files.createDirectories`, `Files.list`, `Files.exists`, etc.) and `java.nio.file.Path` for all path manipulation. This makes the service **storage-location agnostic** — it works identically whether the configured `sharedLogStoragePath` points to a local directory, a mounted network drive (NFS/SMB), AWS EFS, or an S3-backed FUSE mount. No code changes are required to switch between local and remote storage; only the path configuration needs to change.

```java
@Service
public class SharedStorageService {

    /**
     * Uploads a file to the shared storage location using java.nio.file operations.
     * Path structure: {basePath}/{projectName}/{date}/{deviceSerial}/{fileName}
     * Implementation uses Path.resolve() for path construction and Files.copy() for transfer,
     * making it agnostic to whether basePath is local or a mounted remote filesystem.
     * @return the full path where the file was stored, or empty if upload failed
     */
    public Optional<String> upload(Path localFile, String projectName, 
                                    String date, String deviceSerial);

    /**
     * Checks if the shared storage path is accessible using Files.isDirectory() and Files.isWritable().
     */
    public boolean isAccessible(String basePath);

    /**
     * Lists ZIP archives in shared storage uploaded within a time range.
     * Uses Files.walk()/Files.list() for directory traversal.
     */
    public List<Path> listArchives(String basePath, String projectName, 
                                    LocalDate from, LocalDate to);
}
```

### 4. LogAnalysisJob

```java
@Component
public class LogAnalysisJob {

    private final LogUploadMetadataRepository metadataRepository;
    private final SharedStorageService sharedStorageService;
    private final AiAnalysisEngine aiAnalysisEngine;
    private final AnalysisReportRepository reportRepository;
    private final AnalysisJobExecutionRepository executionRepository;
    private final ProjectRepository projectRepository;

    /**
     * Scheduled to run daily. Processes all log archives from the past 24 hours.
     */
    @Scheduled(cron = "${log.analysis.cron:0 0 2 * * *}")
    public void executeAnalysis();
}
```

### 5. AiAnalysisEngine (Interface)

```java
public interface AiAnalysisEngine {

    /**
     * Analyzes a collection of log content grouped by device.
     * Returns a structured analysis report.
     */
    AnalysisResult analyze(String projectName, Map<String, List<String>> logContentByDevice);
}
```

### 6. LogAnalysisController

```java
@RestController
@RequestMapping("/api/analysis")
public class LogAnalysisController {

    @GetMapping("/reports")
    public List<AnalysisReportResponse> getReports(
        @RequestParam Long projectId,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to);

    @GetMapping("/reports/latest")
    public AnalysisReportResponse getLatestReport(@RequestParam Long projectId);

    @GetMapping("/executions")
    public List<AnalysisJobExecutionResponse> getExecutionHistory(
        @RequestParam(defaultValue = "20") int limit);
}
```

### 7. Modified DeviceController (pull-logs endpoint)

The existing `POST /api/devices/{serial}/pull-logs` endpoint will be updated to delegate to `LogCollectionService` instead of calling `LogExportManager` directly. A `projectId` query parameter will be added.

```java
@PostMapping("/{serial}/pull-logs")
public LogCollectionResponse pullLogs(
    @PathVariable String serial,
    @RequestParam(required = false) Long projectId) {
    validateSerial(serial);
    return logCollectionService.collectLogs(serial, projectId);
}
```

## Data Models

### New Entities

#### LogUploadMetadata

```java
@Entity
@Table(name = "log_upload_metadata")
public class LogUploadMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String deviceSerial;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private String filePath;       // full path in shared storage

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false)
    private LocalDate logDate;     // the date folder (YYYY-MM-DD)
}
```

#### AnalysisReport

```java
@Entity
@Table(name = "analysis_reports")
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(columnDefinition = "TEXT")
    private String logStructure;       // JSON: identified patterns

    @Column(columnDefinition = "TEXT")
    private String errorsFound;        // JSON: grouped errors with frequency

    @Column(columnDefinition = "TEXT")
    private String improvements;       // JSON: prioritized suggestions

    @Column(columnDefinition = "TEXT")
    private String riskAssessment;     // JSON: risk level + factors

    @Column(nullable = false)
    private String overallRiskLevel;   // CRITICAL, HIGH, MEDIUM, LOW

    @Column(nullable = false)
    private boolean flaggedForAttention;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private int archivesProcessed;

    @Column(nullable = false)
    private int devicesAnalyzed;
}
```

#### AnalysisJobExecution

```java
@Entity
@Table(name = "analysis_job_executions")
public class AnalysisJobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Column(nullable = false)
    private String status;             // RUNNING, COMPLETED, FAILED

    private int archivesProcessed;

    @Column(columnDefinition = "TEXT")
    private String errorDetails;       // populated on FAILED status
}
```

### Modified Entity

#### Project (add field)

```java
// Add to existing Project entity:
@Column
private String sharedLogStoragePath;  // nullable — if null, shared upload is skipped
```

### DTOs

#### LogCollectionResponse

```java
public record LogCollectionResponse(
    String localPath,
    String exportedLogsFolder,
    String message,
    String sharedStoragePath,  // null if upload skipped/failed
    Long projectId
) {}
```

#### AnalysisReportResponse

```java
public record AnalysisReportResponse(
    Long id,
    Long projectId,
    String projectName,
    LocalDate reportDate,
    LogStructureDto logStructure,
    List<ErrorGroupDto> errorsFound,
    List<ImprovementDto> improvements,
    RiskAssessmentDto riskAssessment,
    String overallRiskLevel,
    boolean flaggedForAttention,
    int archivesProcessed,
    int devicesAnalyzed,
    Instant createdAt
) {}
```

#### AnalysisJobExecutionResponse

```java
public record AnalysisJobExecutionResponse(
    Long id,
    Instant startedAt,
    Instant completedAt,
    String status,
    int archivesProcessed,
    String errorDetails
) {}
```

### Supporting DTOs for Report Sections

```java
public record LogStructureDto(
    List<PatternDto> patterns,
    List<String> newPatterns  // patterns not seen in previous reports
) {}

public record PatternDto(
    String name,
    String timestampFormat,
    String logLevel,
    String component,
    String messageStructure,
    int occurrenceCount
) {}

public record ErrorGroupDto(
    String rootCause,
    String component,
    String description,
    int frequency,
    List<String> affectedDevices,
    boolean widespread  // appears on 2+ devices
) {}

public record ImprovementDto(
    String priority,  // HIGH, MEDIUM, LOW
    String category,
    String recommendation,
    String evidence
) {}

public record RiskAssessmentDto(
    String riskLevel,  // CRITICAL, HIGH, MEDIUM, LOW
    List<String> contributingFactors,
    List<String> recommendedActions
) {}
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Shared storage path construction

*For any* valid project name, date, and device serial, the constructed shared storage path SHALL match the pattern `{projectName}/{YYYY-MM-DD}/{deviceSerial}/logs_{HH-mm-ss}.zip` where the date component is ISO 8601 formatted and the filename uses the hour-minute-second timestamp pattern.

**Validates: Requirements 2.3, 2.4, 2.5**

### Property 2: ZIP archive round-trip

*For any* set of log files with arbitrary content, creating a ZIP archive and then extracting it SHALL produce files with identical names and byte-for-byte identical content to the originals.

**Validates: Requirements 2.1, 5.3**

### Property 3: Upload metadata completeness

*For any* successful shared storage upload, the persisted LogUploadMetadata SHALL have non-null values for project, deviceSerial, uploadedAt, filePath, fileSizeBytes, and logDate, and the fileSizeBytes SHALL equal the actual size of the uploaded archive.

**Validates: Requirements 2.7, 4.3**

### Property 4: 24-hour window filtering and project grouping

*For any* set of LogUploadMetadata records with various timestamps and project associations, the analysis job SHALL select exactly those records where uploadedAt is within the past 24 hours, and SHALL group the selected records by project such that each group contains only records belonging to that project.

**Validates: Requirements 5.2, 5.4**

### Property 5: Analysis report structure completeness

*For any* completed analysis, the persisted AnalysisReport SHALL contain non-null logStructure, errorsFound, improvements, and riskAssessment sections, and the overallRiskLevel SHALL be one of CRITICAL, HIGH, MEDIUM, or LOW, and each improvement suggestion's priority SHALL be one of HIGH, MEDIUM, or LOW.

**Validates: Requirements 6.2, 7.3, 8.2, 8.3, 9.1, 9.2, 9.3**

### Property 6: New pattern detection

*For any* set of previously identified log patterns and a set of currently identified patterns, the report's newPatterns list SHALL contain exactly those patterns present in the current set but absent from the previous set.

**Validates: Requirements 6.3**

### Property 7: Widespread error classification

*For any* error group, the widespread flag SHALL be true if and only if the number of affected devices is greater than or equal to 2.

**Validates: Requirements 7.4**

### Property 8: Risk-based attention flagging

*For any* AnalysisReport, the flaggedForAttention field SHALL be true if and only if the overallRiskLevel is CRITICAL or HIGH.

**Validates: Requirements 9.4**

### Property 9: Report date-range query correctness

*For any* set of AnalysisReports with various reportDates and project associations, querying by project and date range [from, to] SHALL return exactly those reports where the report's project matches and reportDate is within the inclusive range, and the "latest" query SHALL return the report with the maximum reportDate for that project.

**Validates: Requirements 10.2, 10.3**

### Property 10: Project detection from installed packages

*For any* device with a list of installed packages, if exactly one package matches the known SafePath package list, the Log_Collection_Service SHALL associate the log pull with the project corresponding to that package. If no package matches, the result SHALL indicate "Unassigned".

**Validates: Requirements 4.1, 4.2**

## Error Handling

| Scenario | Behavior | User Impact |
|----------|----------|-------------|
| Shared storage unreachable | Log warning, skip upload, return success with `sharedStoragePath = null` | User sees successful local save; no disruption |
| ZIP creation fails | Log error, skip upload, return success for local save | Same as above — local save is primary |
| Project detection fails (no SafePath package) | Use "Unassigned" category or prompt user via `projectId` parameter | User can manually specify project |
| AI analysis engine unavailable | Mark job execution as FAILED with error details | Admin sees failure in execution history |
| AI analysis returns malformed response | Log error, skip report for that project, continue with others | Partial results for other projects still saved |
| Database write failure during metadata save | Log error, do not fail the pull-logs response | Upload metadata lost but logs are still on shared storage |
| Overnight job overlaps (previous still running) | Skip execution, log warning | Next scheduled run will pick up unprocessed archives |

### Retry Strategy

- **Shared storage upload**: No automatic retry. The file remains in the local temp directory for manual recovery. A future enhancement could add retry with exponential backoff.
- **AI analysis**: Per-project retry (up to 2 attempts). If a project's analysis fails, other projects continue.
- **Database writes**: Standard Spring transaction rollback. No custom retry.

## Testing Strategy

### Unit Tests (Example-Based)

- `LogCollectionService`: Verify local save is called, ZIP is created, upload is attempted, metadata is saved
- `LogCollectionService`: Verify graceful degradation when shared storage is unreachable (Req 2.6)
- `LogCollectionService`: Verify skip behavior when project has no shared storage path (Req 3.2)
- `LogAnalysisJob`: Verify job execution lifecycle (RUNNING → COMPLETED/FAILED) (Req 11.1-11.3)
- `LogAnalysisController`: Verify authentication enforcement (Req 10.4)
- `LogAnalysisController`: Verify endpoint responses match expected DTOs

### Property-Based Tests

Property-based tests use **jqwik** (Java property-based testing library compatible with JUnit 5) with a minimum of 100 iterations per property.

| Property | Test Class | What's Generated |
|----------|-----------|-----------------|
| Property 1: Path construction | `SharedStoragePathPropertyTest` | Random project names, LocalDate values, device serials, LocalTime values |
| Property 2: ZIP round-trip | `ZipArchivePropertyTest` | Random file names and byte[] contents |
| Property 3: Metadata completeness | `LogUploadMetadataPropertyTest` | Random project/device/file combinations |
| Property 4: 24-hour filtering | `AnalysisJobFilteringPropertyTest` | Random sets of metadata with timestamps spanning multiple days |
| Property 5: Report completeness | `AnalysisReportStructurePropertyTest` | Random AnalysisResult objects |
| Property 6: New pattern detection | `PatternDetectionPropertyTest` | Random sets of pattern strings (previous and current) |
| Property 7: Widespread classification | `WidespreadErrorPropertyTest` | Random error groups with varying device lists |
| Property 8: Attention flagging | `AttentionFlaggingPropertyTest` | Random risk levels |
| Property 9: Date-range query | `ReportQueryPropertyTest` | Random reports with various dates, random query ranges |
| Property 10: Project detection | `ProjectDetectionPropertyTest` | Random package lists with/without SafePath packages |

**Configuration:**
- Library: `net.jqwik:jqwik:1.9.1` (add to `build.gradle` test dependencies)
- Minimum iterations: 100 per property
- Tag format: `// Feature: log-collection-analysis, Property {N}: {title}`

### Integration Tests

- End-to-end pull-logs flow with mocked device and real file system
- Scheduled job execution with test data in H2 database
- REST endpoint tests using `@WebMvcTest` with authentication
- Shared storage upload/download with a temp directory simulating network path
