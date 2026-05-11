package androidtoolkit.backend.dto;

public record PatternDto(
    String name,
    String timestampFormat,
    String logLevel,
    String component,
    String messageStructure,
    int occurrenceCount
) {}
