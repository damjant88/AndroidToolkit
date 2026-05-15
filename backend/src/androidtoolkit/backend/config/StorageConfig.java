package androidtoolkit.backend.config;

import androidtoolkit.backend.service.LocalFilesystemStorageService;
import androidtoolkit.backend.service.ObjectStorageService;
import androidtoolkit.backend.service.S3ObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally creates the appropriate ObjectStorageService bean
 * based on the DEPLOYMENT_MODE environment variable.
 */
@Configuration
public class StorageConfig {

    @Value("${deployment.mode:standalone}")
    private String deploymentMode;

    @Value("${storage.endpoint:}")
    private String storageEndpoint;

    @Value("${storage.bucket:androidtoolkit}")
    private String storageBucket;

    @Value("${storage.access-key:}")
    private String storageAccessKey;

    @Value("${storage.secret-key:}")
    private String storageSecretKey;

    @Value("${storage.local.base-dir:./data/storage}")
    private String localBaseDir;

    @Bean
    public ObjectStorageService objectStorageService() {
        if ("saas".equalsIgnoreCase(deploymentMode) && !storageEndpoint.isBlank()) {
            return new S3ObjectStorageService(storageEndpoint, storageBucket, storageAccessKey, storageSecretKey);
        }
        return new LocalFilesystemStorageService(localBaseDir);
    }
}
