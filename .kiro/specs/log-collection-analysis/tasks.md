# Implementation Plan: Log Collection & Analysis

## Overview

This plan implements centralized log storage and automated AI-driven analysis for the AndroidToolkit. The implementation wraps the existing `LogExportManager` with a new `LogCollectionService` that adds ZIP archiving, shared storage upload, metadata persistence, and a scheduled overnight AI analysis job. All new code lives in the `backend` module; the `core` module remains untouched.

## Tasks

- [x] 1. Define data models, DTOs, and repository interfaces
  - [x] 1.1 Add `sharedLogStoragePath` field to the existing `Project` entity
    - Add a nullable `@Column private String sharedLogStoragePath` field to `backend/src/androidtoolkit/backend/entity/Project.java`
    - Add getter and setter methods
    - _Requirements: 3.1_

  - [x] 1.2 Create `LogUploadMetadata` entity
    - Create `backend/src/androidtoolkit/backend/entity/LogUploadMetadata.java`
    - Fields: id, project (ManyToOne), deviceSerial, uploadedAt, filePath, fileSizeBytes, logDate
    - _Requirements: 2.7, 4.3_

  - [x] 1.3 Create `AnalysisReport` entity
    - Create `backend/src/androidtoolkit/backend/entity/AnalysisReport.java`
    - Fields: id, project, reportDate, logStructure (TEXT), errorsFound (TEXT), improvements (TEXT), riskAssessment (TEXT), overallRiskLevel, flaggedForAttention, createdAt, archivesProcessed, devicesAnalyzed
    - _Requirements: 10.1_

  - [x] 1.4 Create `AnalysisJobExecution` entity
    - Create `backend/src/androidtoolkit/backend/entity/AnalysisJobExecution.java`
    - Fields: id, startedAt, completedAt, status, archivesProcessed, errorDetails (TEXT)
    - _Requirements: 11.1, 11.2, 11.3_

  - [x] 1.5 Create repository interfaces
    - Create `LogUploadMetadataRepository.java` with query methods: `findByUploadedAtBetween`, `findByProjectAndUploadedAtBetween`
    - Create `AnalysisReportRepository.java` with query methods: `findByProjectIdAndReportDateBetween`, `findFirstByProjectIdOrderByReportDateDesc`
    - Create `AnalysisJobExecutionRepository.java` with query method: `findTopNByOrderByStartedAtDesc`
    - _Requirements: 2.7, 5.2, 10.2, 10.3, 11.4_

  - [x] 1.6 Create DTOs and response records
    - Create `LogCollectionResponse.java` record (localPath, exportedLogsFolder, message, sharedStoragePath, projectId)
    - Create `AnalysisReportResponse.java` record with nested DTOs
    - Create `AnalysisJobExecutionResponse.java` record
    - Create supporting DTOs: `LogStructureDto`, `PatternDto`, `ErrorGroupDto`, `ImprovementDto`, `RiskAssessmentDto`
    - _Requirements: 1.3, 10.2, 11.4_

- [x] 2. Implement ZIP archive and shared storage services
  - [x] 2.1 Implement `ZipArchiveService`
    - Create `backend/src/androidtoolkit/backend/service/ZipArchiveService.java`
    - Implement `createArchive(Path sourceDir, String archiveName)` using `java.util.zip.ZipOutputStream`
    - Handle empty directories and nested files
    - _Requirements: 2.1_

  - [ ]* 2.2 Write property test for ZIP round-trip (Property 2)
    - **Property 2: ZIP archive round-trip**
    - Create `ZipArchivePropertyTest.java` using jqwik
    - Generate random file names and byte[] contents, verify round-trip produces identical files
    - Add `net.jqwik:jqwik:1.9.1` to `backend/build.gradle` test dependencies
    - **Validates: Requirements 2.1, 5.3**

  - [x] 2.3 Implement `SharedStorageService`
    - Create `backend/src/androidtoolkit/backend/service/SharedStorageService.java`
    - Implement `upload(Path localFile, String projectName, String date, String deviceSerial)` with path structure `{basePath}/{projectName}/{date}/{deviceSerial}/{fileName}`
    - Implement `isAccessible(String basePath)` to check path reachability
    - Implement `listArchives(String basePath, String projectName, LocalDate from, LocalDate to)`
    - _Requirements: 2.2, 2.3, 2.4, 2.5_

  - [ ]* 2.4 Write property test for shared storage path construction (Property 1)
    - **Property 1: Shared storage path construction**
    - Create `SharedStoragePathPropertyTest.java` using jqwik
    - Generate random project names, LocalDate values, device serials, LocalTime values
    - Verify path matches pattern `{projectName}/{YYYY-MM-DD}/{deviceSerial}/logs_{HH-mm-ss}.zip`
    - **Validates: Requirements 2.3, 2.4, 2.5**

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement LogCollectionService and update DeviceController
  - [x] 4.1 Implement `LogCollectionService`
    - Create `backend/src/androidtoolkit/backend/service/LogCollectionService.java`
    - Inject `LogExportManager`, `AppServices`, `ZipArchiveService`, `SharedStorageService`, `LogUploadMetadataRepository`, `ProjectRepository`, `DeviceGateway`, `PackageClassifier`
    - Implement `collectLogs(String serial, Long projectId)`:
      1. Call `logExportManager.exportDeviceLogs()` for local save
      2. Create ZIP archive from exported folder
      3. Determine project association (from installed packages or provided projectId)
      4. Upload ZIP to shared storage asynchronously (use `@Async`)
      5. Persist `LogUploadMetadata` on successful upload
      6. Return `LogCollectionResponse` immediately after local save
    - Handle graceful degradation: if shared storage unreachable or project has no path, skip upload
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.6, 2.7, 3.2, 4.1, 4.2_

  - [ ]* 4.2 Write property test for upload metadata completeness (Property 3)
    - **Property 3: Upload metadata completeness**
    - Create `LogUploadMetadataPropertyTest.java` using jqwik
    - Generate random project/device/file combinations
    - Verify all non-null constraints and fileSizeBytes matches actual archive size
    - **Validates: Requirements 2.7, 4.3**

  - [ ]* 4.3 Write property test for project detection from installed packages (Property 10)
    - **Property 10: Project detection from installed packages**
    - Create `ProjectDetectionPropertyTest.java` using jqwik
    - Generate random package lists with/without SafePath packages
    - Verify correct project association or "Unassigned" result
    - **Validates: Requirements 4.1, 4.2**

  - [x] 4.4 Update `DeviceController.pullLogs` endpoint
    - Modify `POST /api/devices/{serial}/pull-logs` to accept optional `@RequestParam Long projectId`
    - Delegate to `LogCollectionService.collectLogs()` instead of calling `LogExportManager` directly
    - Change return type from `LogExportResponse` to `LogCollectionResponse`
    - _Requirements: 1.1, 1.2, 1.3, 4.2_

  - [ ]* 4.5 Write unit tests for LogCollectionService
    - Test local save is called and response returned immediately
    - Test graceful degradation when shared storage is unreachable (Req 2.6)
    - Test skip behavior when project has no shared storage path (Req 3.2)
    - Test project detection from device packages
    - _Requirements: 1.1, 1.2, 1.3, 2.6, 3.2, 4.1_

- [x] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement AI analysis engine interface and overnight job
  - [x] 6.1 Create `AiAnalysisEngine` interface and `AnalysisResult` model
    - Create `backend/src/androidtoolkit/backend/service/AiAnalysisEngine.java` interface
    - Define `AnalysisResult analyze(String projectName, Map<String, List<String>> logContentByDevice)`
    - Create `AnalysisResult.java` with fields for logStructure, errorsFound, improvements, riskAssessment
    - _Requirements: 5.3, 6.1, 7.1, 8.1, 9.1_

  - [x] 6.2 Implement `LogAnalysisJob`
    - Create `backend/src/androidtoolkit/backend/job/LogAnalysisJob.java`
    - Use `@Scheduled(cron = "${log.analysis.cron:0 0 2 * * *}")` for configurable overnight execution
    - Implement `executeAnalysis()`:
      1. Record job start (RUNNING status) in `AnalysisJobExecution`
      2. Query `LogUploadMetadataRepository` for records from past 24 hours
      3. Group records by project
      4. For each project: read ZIPs from shared storage, extract log content, call `AiAnalysisEngine`
      5. Map `AnalysisResult` to `AnalysisReport` entity and persist
      6. Set `flaggedForAttention` based on risk level (CRITICAL or HIGH → true)
      7. Detect new patterns by comparing with previous report
      8. Update job execution status (COMPLETED/FAILED)
    - Handle per-project retry (up to 2 attempts)
    - Skip execution if previous job still running
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4, 11.1, 11.2, 11.3_

  - [ ]* 6.3 Write property test for 24-hour window filtering (Property 4)
    - **Property 4: 24-hour window filtering and project grouping**
    - Create `AnalysisJobFilteringPropertyTest.java` using jqwik
    - Generate random sets of metadata with timestamps spanning multiple days
    - Verify exactly those records within 24 hours are selected and grouped correctly by project
    - **Validates: Requirements 5.2, 5.4**

  - [ ]* 6.4 Write property test for analysis report structure completeness (Property 5)
    - **Property 5: Analysis report structure completeness**
    - Create `AnalysisReportStructurePropertyTest.java` using jqwik
    - Generate random AnalysisResult objects
    - Verify non-null sections and valid enum values for risk/priority levels
    - **Validates: Requirements 6.2, 7.3, 8.2, 8.3, 9.1, 9.2, 9.3**

  - [ ]* 6.5 Write property test for new pattern detection (Property 6)
    - **Property 6: New pattern detection**
    - Create `PatternDetectionPropertyTest.java` using jqwik
    - Generate random sets of previous and current pattern strings
    - Verify newPatterns = current - previous (set difference)
    - **Validates: Requirements 6.3**

  - [ ]* 6.6 Write property test for widespread error classification (Property 7)
    - **Property 7: Widespread error classification**
    - Create `WidespreadErrorPropertyTest.java` using jqwik
    - Generate random error groups with varying device lists
    - Verify widespread flag is true iff affectedDevices.size() >= 2
    - **Validates: Requirements 7.4**

  - [ ]* 6.7 Write property test for risk-based attention flagging (Property 8)
    - **Property 8: Risk-based attention flagging**
    - Create `AttentionFlaggingPropertyTest.java` using jqwik
    - Generate random risk levels
    - Verify flaggedForAttention is true iff overallRiskLevel is CRITICAL or HIGH
    - **Validates: Requirements 9.4**

  - [ ]* 6.8 Write unit tests for LogAnalysisJob
    - Test job execution lifecycle (RUNNING → COMPLETED/FAILED)
    - Test skip behavior when previous job still running
    - Test per-project retry on AI engine failure
    - Test partial results when one project fails
    - _Requirements: 11.1, 11.2, 11.3_

- [x] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement REST endpoints for analysis reports and job monitoring
  - [x] 8.1 Create `LogAnalysisController`
    - Create `backend/src/androidtoolkit/backend/controller/LogAnalysisController.java`
    - Implement `GET /api/analysis/reports` with params: projectId, from (date), to (date)
    - Implement `GET /api/analysis/reports/latest` with param: projectId
    - Implement `GET /api/analysis/executions` with param: limit (default 20)
    - All endpoints require authentication (Spring Security)
    - Map entities to response DTOs
    - _Requirements: 10.2, 10.3, 10.4, 11.4_

  - [ ]* 8.2 Write property test for report date-range query correctness (Property 9)
    - **Property 9: Report date-range query correctness**
    - Create `ReportQueryPropertyTest.java` using jqwik
    - Generate random reports with various dates and project associations, random query ranges
    - Verify query returns exactly matching reports and "latest" returns max reportDate
    - **Validates: Requirements 10.2, 10.3**

  - [ ]* 8.3 Write unit tests for LogAnalysisController
    - Test authentication enforcement on all endpoints
    - Test response DTO mapping
    - Test date range filtering
    - Test latest report retrieval
    - Use `@WebMvcTest` with mocked service layer
    - _Requirements: 10.2, 10.3, 10.4, 11.4_

- [x] 9. Configuration and wiring
  - [x] 9.1 Add configuration properties and async support
    - Add `log.analysis.cron` property to `application.properties` with default `0 0 2 * * *`
    - Enable `@EnableAsync` and `@EnableScheduling` in application config
    - Add jqwik test dependency to `backend/build.gradle`: `testImplementation 'net.jqwik:jqwik:1.9.1'`
    - _Requirements: 5.1, 3.3_

  - [x] 9.2 Wire LogCollectionService into Spring context
    - Ensure `LogCollectionService` is properly injected into `DeviceController`
    - Update `CoreServicesConfig` if needed to expose `PackageClassifier` and `DeviceGateway` as beans
    - Verify all new services are discoverable via component scanning
    - _Requirements: 1.1, 4.1_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases
- The `core` module is NOT modified — all new code lives in `backend`
- The existing `LogExportManager` behavior is preserved; `LogCollectionService` wraps it
- AI analysis engine is abstracted behind an interface for implementation flexibility

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "1.6"] },
    { "id": 1, "tasks": ["1.5", "2.1", "6.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "9.1"] },
    { "id": 3, "tasks": ["2.4", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 5, "tasks": ["4.5", "6.2", "9.2"] },
    { "id": 6, "tasks": ["6.3", "6.4", "6.5", "6.6", "6.7", "6.8"] },
    { "id": 7, "tasks": ["8.1"] },
    { "id": 8, "tasks": ["8.2", "8.3"] }
  ]
}
```
