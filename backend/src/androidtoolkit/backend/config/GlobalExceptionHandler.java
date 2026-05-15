package androidtoolkit.backend.config;

import androidtoolkit.backend.service.TierLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("success", false, "message", e.getReason() != null ? e.getReason() : "Error"));
    }

    @ExceptionHandler(TierLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTierLimitExceeded(TierLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "error", "TIER_LIMIT_EXCEEDED",
                        "message", e.getMessage(),
                        "details", Map.of(
                                "limit", e.getLimit(),
                                "current", e.getCurrent(),
                                "tier", e.getTier().name(),
                                "resource", e.getResource()
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }
}
