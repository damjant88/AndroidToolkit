# Requirements Document

## Introduction

This feature extends the existing "Pull SP Logs" functionality in AndroidToolkit to include centralized log storage, shared access across team members, and automated overnight AI-driven log analysis. When a user pulls logs from a device, the logs are saved locally for the user and also zipped and uploaded to a shared network location organized by project, date, and device. An overnight scheduled job analyzes the collected logs using AI to learn log structure, detect errors, suggest improvements, and assess risks.

## Glossary

- **Log_Collection_Service**: The backend service responsible for saving pulled logs locally and uploading them to the shared storage location
- **Shared_Storage**: A configurable network or file-system location where zipped log archives are stored, organized by project, date, and device
- **Log_Archive**: A ZIP file containing all log files pulled from a single device during a single pull operation
- **AI_Analysis_Job**: A scheduled overnight background job that processes logs collected in the past 24 hours using an AI model
- **Analysis_Report**: The output of the AI_Analysis_Job containing structure insights, errors found, improvement suggestions, and risk assessments
- **Project**: An existing entity in the system representing a product (e.g., ATT Project/FamilySecurity, SafePath project) with name, remoteApkLocation, and localApkFolder fields
- **Device_Serial**: The unique serial identifier of an Android device connected via ADB
- **Log_Export_Manager**: The existing core service that exports device logs from an Android device to a target folder

## Requirements

### Requirement 1: Local Log Storage

**User Story:** As a tester, I want pulled logs to be saved locally on my machine, so that I can access them immediately without network dependency.

#### Acceptance Criteria

1. WHEN a user clicks "Pull SP Logs" for a device, THE Log_Collection_Service SHALL save the exported log files to the user's local logs directory
2. THE Log_Collection_Service SHALL preserve the existing local save behavior provided by the Log_Export_Manager
3. WHEN the local save completes, THE Log_Collection_Service SHALL return the local file path in the response to the user

### Requirement 2: Shared Storage Upload

**User Story:** As a team lead, I want all pulled logs uploaded to a shared location, so that any team member can access historical logs for any device and project.

#### Acceptance Criteria

1. WHEN a user clicks "Pull SP Logs" for a device, THE Log_Collection_Service SHALL create a ZIP archive containing all exported log files from that pull operation
2. WHEN the ZIP archive is created, THE Log_Collection_Service SHALL upload the archive to the Shared_Storage location
3. THE Log_Collection_Service SHALL organize uploaded archives in the Shared_Storage using the path structure: `{project_name}/{date}/{device_serial}/`
4. THE Log_Collection_Service SHALL use the ISO 8601 date format (YYYY-MM-DD) for the date folder name
5. THE Log_Collection_Service SHALL name each archive file with a timestamp pattern: `logs_{HH-mm-ss}.zip`
6. IF the Shared_Storage location is unreachable, THEN THE Log_Collection_Service SHALL log a warning and still return a successful response for the local save
7. WHEN the upload completes, THE Log_Collection_Service SHALL record the upload metadata (project, device, timestamp, file path, file size) in the database

### Requirement 3: Shared Storage Configuration

**User Story:** As an administrator, I want to configure the shared storage location per project, so that different teams can use their own storage paths.

#### Acceptance Criteria

1. THE Project entity SHALL include a configurable shared log storage path field
2. WHEN a project has no shared log storage path configured, THE Log_Collection_Service SHALL skip the shared upload and log an informational message
3. WHEN an administrator updates the shared log storage path, THE Log_Collection_Service SHALL use the new path for subsequent uploads without requiring a restart

### Requirement 4: Project Association for Log Pulls

**User Story:** As a tester, I want pulled logs to be associated with the correct project, so that logs are organized by the project I am working on.

#### Acceptance Criteria

1. WHEN a user pulls logs from a device, THE Log_Collection_Service SHALL determine the associated project based on the installed SafePath package on the device
2. IF no project can be determined from the device, THEN THE Log_Collection_Service SHALL prompt the user to select a project or use a default "Unassigned" category
3. THE Log_Collection_Service SHALL include the project association in the upload metadata

### Requirement 5: Overnight AI Analysis Job

**User Story:** As a team lead, I want an automated overnight job that analyzes collected logs, so that I receive insights about errors, risks, and improvements without manual review.

#### Acceptance Criteria

1. THE AI_Analysis_Job SHALL execute once daily at a configurable time (default: 02:00 AM server time)
2. WHEN the AI_Analysis_Job executes, THE AI_Analysis_Job SHALL process all log archives uploaded to Shared_Storage within the past 24 hours
3. WHEN processing a log archive, THE AI_Analysis_Job SHALL extract and parse the log files from the ZIP archive
4. THE AI_Analysis_Job SHALL analyze logs grouped by project

### Requirement 6: Log Structure Learning

**User Story:** As a developer, I want the AI to learn the structure of our log files, so that it can provide context-aware analysis.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job processes logs for a project, THE AI_Analysis_Job SHALL identify recurring log patterns including timestamp formats, log levels, component names, and message structures
2. THE AI_Analysis_Job SHALL include a "Log Structure" section in the Analysis_Report describing the identified patterns
3. WHEN a new log pattern is detected that differs from previously identified patterns, THE AI_Analysis_Job SHALL flag the new pattern in the report

### Requirement 7: Error Detection

**User Story:** As a developer, I want the AI to find errors in the logs, so that I can quickly identify issues without reading through entire log files.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job processes logs, THE AI_Analysis_Job SHALL identify all error-level and fatal-level log entries
2. THE AI_Analysis_Job SHALL group related errors by root cause or component
3. THE AI_Analysis_Job SHALL include an "Errors Found" section in the Analysis_Report with error descriptions, frequency counts, and affected devices
4. WHEN an error appears across multiple devices, THE AI_Analysis_Job SHALL flag the error as a widespread issue

### Requirement 8: Improvement Suggestions

**User Story:** As a developer, I want the AI to suggest improvements based on log patterns, so that I can proactively improve application quality.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job completes error detection, THE AI_Analysis_Job SHALL generate improvement suggestions based on detected patterns
2. THE AI_Analysis_Job SHALL categorize suggestions by priority (high, medium, low)
3. THE AI_Analysis_Job SHALL include an "Improvements" section in the Analysis_Report with actionable recommendations
4. THE AI_Analysis_Job SHALL base suggestions on patterns such as repeated warnings, performance degradation indicators, and deprecated API usage

### Requirement 9: Risk Assessment

**User Story:** As a team lead, I want the AI to assess risks from the logs, so that I can prioritize issues before they impact users.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job completes analysis, THE AI_Analysis_Job SHALL produce a risk assessment for each project
2. THE AI_Analysis_Job SHALL assign a risk level (critical, high, medium, low) based on error severity, frequency, and spread across devices
3. THE AI_Analysis_Job SHALL include a "Risk Assessment" section in the Analysis_Report with risk level, contributing factors, and recommended actions
4. WHEN the risk level is critical or high, THE AI_Analysis_Job SHALL flag the report for immediate attention

### Requirement 10: Analysis Report Storage and Access

**User Story:** As a team member, I want to view AI analysis reports, so that I can act on the findings.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job completes, THE AI_Analysis_Job SHALL persist the Analysis_Report in the database associated with the project and execution date
2. THE System SHALL expose an API endpoint to retrieve Analysis_Reports filtered by project and date range
3. THE System SHALL expose an API endpoint to retrieve the most recent Analysis_Report for a given project
4. WHEN a user requests an Analysis_Report, THE System SHALL verify the user is authenticated before returning the report

### Requirement 11: Analysis Job Monitoring

**User Story:** As an administrator, I want to monitor the AI analysis job execution, so that I can verify it runs successfully.

#### Acceptance Criteria

1. WHEN the AI_Analysis_Job starts execution, THE AI_Analysis_Job SHALL record the start time and status as "RUNNING" in the database
2. WHEN the AI_Analysis_Job completes, THE AI_Analysis_Job SHALL update the status to "COMPLETED" with the end time and number of archives processed
3. IF the AI_Analysis_Job encounters an unrecoverable error, THEN THE AI_Analysis_Job SHALL update the status to "FAILED" with the error details
4. THE System SHALL expose an API endpoint to retrieve the execution history of the AI_Analysis_Job
