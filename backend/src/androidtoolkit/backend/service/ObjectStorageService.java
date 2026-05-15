package androidtoolkit.backend.service;

import java.io.InputStream;

/**
 * Abstraction for cloud-agnostic object storage operations.
 * Implementations include S3-compatible storage (AWS S3, MinIO, GCP, Azure)
 * and local filesystem storage for standalone mode.
 */
public interface ObjectStorageService {

    /**
     * Upload a file to a tenant-scoped path in object storage.
     *
     * @param tenantId      the owning tenant's identifier
     * @param projectPrefix the project-specific path prefix
     * @param filename      the file name
     * @param data          the file content stream
     * @param size          the file size in bytes
     * @return the full object key where the file was stored
     */
    String upload(Long tenantId, String projectPrefix, String filename, InputStream data, long size);

    /**
     * Download a file from object storage.
     *
     * @param objectKey the full object key
     * @return an input stream of the file content
     */
    InputStream download(String objectKey);

    /**
     * Archive (soft-delete) a file in object storage.
     *
     * @param objectKey the full object key to archive
     */
    void archive(String objectKey);

    /**
     * Calculate the total storage usage for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return total bytes used by the tenant
     */
    long getTenantStorageUsage(Long tenantId);
}
