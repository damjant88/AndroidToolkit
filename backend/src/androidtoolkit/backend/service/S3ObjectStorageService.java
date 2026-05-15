package androidtoolkit.backend.service;

import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;

/**
 * S3-compatible implementation of ObjectStorageService.
 * Works with AWS S3, MinIO, GCP Cloud Storage (S3 compat), and Azure Blob (S3 compat).
 */
public class S3ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorageService(
            @Value("${storage.endpoint}") String endpoint,
            @Value("${storage.bucket}") String bucket,
            @Value("${storage.access-key}") String accessKey,
            @Value("${storage.secret-key}") String secretKey
    ) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public String upload(Long tenantId, String projectPrefix, String filename, InputStream data, long size) {
        String key = buildKey(tenantId, projectPrefix, filename);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentLength(size)
                        .build(),
                RequestBody.fromInputStream(data, size)
        );
        return key;
    }

    @Override
    public InputStream download(String objectKey) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .build()
        );
    }

    @Override
    public void archive(String objectKey) {
        String archiveKey = "archived/" + objectKey;
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(objectKey)
                .destinationBucket(bucket)
                .destinationKey(archiveKey)
                .build());
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }

    @Override
    public long getTenantStorageUsage(Long tenantId) {
        String prefix = "tenants/" + tenantId + "/";
        long totalSize = 0;
        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix);
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            for (S3Object obj : response.contents()) {
                totalSize += obj.size();
            }
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (continuationToken != null);

        return totalSize;
    }

    private String buildKey(Long tenantId, String projectPrefix, String filename) {
        return "tenants/" + tenantId + "/projects/" + projectPrefix + "/" + filename;
    }
}
