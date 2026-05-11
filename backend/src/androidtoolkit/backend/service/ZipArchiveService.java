package androidtoolkit.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service for creating ZIP archives from directories of log files.
 */
@Service
public class ZipArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ZipArchiveService.class);

    /**
     * Creates a ZIP archive from a directory of log files.
     * The archive is created in the same parent directory as the source directory.
     * Files are stored with paths relative to the source directory.
     *
     * @param sourceDir   the directory containing exported log files
     * @param archiveName the name for the ZIP file (e.g., "logs_14-30-00.zip")
     * @return Path to the created ZIP file
     * @throws IOException if an I/O error occurs during archive creation
     */
    public Path createArchive(Path sourceDir, String archiveName) throws IOException {
        if (sourceDir == null || archiveName == null || archiveName.isBlank()) {
            throw new IllegalArgumentException("sourceDir and archiveName must not be null or blank");
        }

        Path archivePath = sourceDir.getParent().resolve(archiveName);
        log.debug("Creating ZIP archive: {} from directory: {}", archivePath, sourceDir);

        try (OutputStream fos = Files.newOutputStream(archivePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                log.warn("Source directory does not exist or is not a directory: {}", sourceDir);
                zos.finish();
                return archivePath;
            }

            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.filter(Files::isRegularFile)
                        .forEach(file -> addFileToZip(zos, sourceDir, file));
            }
        }

        log.info("ZIP archive created: {} ({} bytes)", archivePath, Files.size(archivePath));
        return archivePath;
    }

    private void addFileToZip(ZipOutputStream zos, Path sourceDir, Path file) {
        String relativePath = sourceDir.relativize(file).toString().replace("\\", "/");
        try {
            ZipEntry entry = new ZipEntry(relativePath);
            zos.putNextEntry(entry);

            try (InputStream is = Files.newInputStream(file)) {
                is.transferTo(zos);
            }

            zos.closeEntry();
        } catch (IOException e) {
            log.error("Failed to add file to ZIP archive: {}", relativePath, e);
            throw new RuntimeException("Failed to add file to ZIP: " + relativePath, e);
        }
    }
}
