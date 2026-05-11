package androidtoolkit.backend.dto;

import java.util.List;

public record LogStructureDto(
    List<PatternDto> patterns,
    List<String> newPatterns
) {}
