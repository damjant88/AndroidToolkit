package androidtoolkit.backend.service;

import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Local filesystem implementation of ObjectStorageService for standalone mode.
 * Stores files under a configurable base directory with tenant-scoped paths.
 */
public class LocalFilesystemStorageService implements ObjectStorageService {

    private final Path baseDir;

    public LocalFilesystemStorageService(@Value("${storage.local.base-dir:./data/storage}") String baseDir) {
        this.baseDir = Paths.get(baseDir);
    }

    @Override
    public String upload(Long tenantId, String projectPrefix, String filename, InputStream data, long size) {
        String key = buildKey(tenantId, projectPrefix, filename);
        Path filePath = baseDir.resolve(key);
        try {
            Files.createDirectories(filePath.getParent());
            Files.copy(data, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload file: " + key, e);
        }
        return key;
    }

    @Override
    public InputStream download(String objectKey) {
        Path filePath = baseDir.resolve(objectKey);
        try {
            return new FileInputStream(filePath.toFile());
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException("File not found: " + objectKey, e);
        }
    }

    @Override
    public void archive(String objectKey) {
        Path source = baseDir.resolve(objectKey);
        Path archivePath = baseDir.resolve("archived/" + objectKey);
        try {
            Files.createDirectories(archivePath.getParent());
            Files.move(source, archivePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to archive file: " + objectKey, e);
        }
    }

    @Override
    public long getTenantStorageUsage(Long tenantId) {
        Path tenantDir = baseDir.resolve("tenants/" + tenantId);
        if (!Files.exists(tenantDir)) {
            return 0;
        }
        AtomicLong totalSize = new AtomicLong(0);
        try {
            Files.walkFileTree(tenantDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    totalSize.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to calculate storage usage", e);
        }
        return totalSize.get();
    }

    private String buildKey(Long tenantId, String projectPrefix, String filename) {
        return "tenants/" + tenantId + "/projects/" + projectPrefix + "/" + filename;
    }
}
