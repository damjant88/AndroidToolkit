# ADR-004: Input Validation to Prevent Command Injection

## Status
Accepted

## Context
The backend passes user-provided serial numbers and file paths directly to `adb` commands via `CommandExecutor.runCommand()`. Without validation, a malicious serial like `; rm -rf /` would execute arbitrary shell commands.

## Decision
Introduce `InputValidator` with centralized validation:
- **Serial numbers** must match `[a-zA-Z0-9._:-]+` (rejects shell metacharacters)
- **Package names** must match `[a-zA-Z0-9._]+`
- **File paths** must not contain `..` (prevents path traversal)
- **Sensitive paths** (recordings, open-folder) must be within `C:/AdbToolkit/`
- **Upload filenames** are sanitized — path components stripped, only safe characters kept

A `GlobalExceptionHandler` converts `IllegalArgumentException` to HTTP 400 responses.

## Consequences
- Command injection is blocked at the controller level
- Path traversal attacks are prevented
- Uploaded files can't escape the uploads directory
- All validation is in one class — easy to audit and extend
- Validation errors return clear 400 messages to the client
