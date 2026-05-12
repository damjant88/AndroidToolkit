package androidtoolkit.backend.service;

/**
 * A single field extracted from a logcat line by the parser.
 *
 * @param type  the category of the extracted field
 * @param value the extracted string value
 */
public record ParsedField(FieldType type, String value) {}
