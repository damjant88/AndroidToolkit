package androidtoolkit.backend.service;

/**
 * Types of structured fields that can be extracted from logcat OkHttp lines.
 */
public enum FieldType {
    ENVIRONMENT,
    CLIENT_VERSION,
    SERVER_PRODUCT_VERSION,
    SERVER_PROJECT_VERSION,
    ACCESS_TOKEN
}
