package androidtoolkit.backend.dto;

import java.util.List;

public record ErrorGroupDto(
    String rootCause,
    String component,
    String description,
    int frequency,
    List<String> affectedDevices,
    boolean widespread
) {}
