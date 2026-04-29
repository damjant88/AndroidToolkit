package androidtoolkit.backend.validation;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes user input to prevent:
 * - Command injection via serial numbers
 * - Path traversal via file paths and filenames
 */
public class InputValidator {

    // Serial numbers: alphanumeric, dots, colons, dashes, underscores (e.g. "192.168.1.5:5555", "RFXYZ123")
    private static final Pattern SERIAL_PATTERN = Pattern.compile("^[a-zA-Z0-9._:\\-]+$");

    // Package names: alphanumeric, dots, underscores (e.g. "com.example.app")
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-zA-Z0-9._]+$");

    // Allowed root directory for all file operations
    private static final String ALLOWED_ROOT = "C:/AdbToolkit";

    public static void validateSerial(String serial) {
        if (serial == null || serial.isBlank()) {
            throw new IllegalArgumentException("Serial number is required");
        }
        if (!SERIAL_PATTERN.matcher(serial).matches()) {
            throw new IllegalArgumentException("Invalid serial number format: " + serial);
        }
    }

    public static void validatePackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("Package name is required");
        }
        if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
            throw new IllegalArgumentException("Invalid package name format: " + packageName);
        }
    }

    public static void validateFilePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("File path is required");
        }
        // Normalize and check for path traversal
        File file = new File(path).getAbsoluteFile();
        String normalized = file.getPath().replace("\\", "/");
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed");
        }
    }

    public static void validatePathWithinAllowedRoot(String path) {
        validateFilePath(path);
        File file = new File(path).getAbsoluteFile();
        String normalized = file.getPath().replace("\\", "/");
        if (!normalized.startsWith(ALLOWED_ROOT.replace("/", File.separator)) &&
                !normalized.startsWith(ALLOWED_ROOT)) {
            throw new IllegalArgumentException("Path must be within " + ALLOWED_ROOT);
        }
    }

    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        // Strip any path components — only keep the base filename
        String baseName = new File(fileName).getName();
        // Remove any remaining dangerous characters
        baseName = baseName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        if (!baseName.endsWith(".apk")) {
            throw new IllegalArgumentException("Only .apk files are accepted");
        }
        return baseName;
    }
}
