package androidtoolkit.backend.dto;

public record ImprovementDto(
    String priority,
    String category,
    String recommendation,
    String evidence
) {}
