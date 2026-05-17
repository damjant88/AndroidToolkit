package androidtoolkit.backend.crash;

import java.util.Deque;
import java.util.List;

/**
 * Spring application event published when a crash is detected.
 * Contains the crash event plus the line buffer and post-crash lines
 * needed for log capture.
 */
public record CrashDetectedEvent(
    CrashEvent crashEvent,
    Deque<String> lineBuffer,
    List<String> postCrashLines
) {}
