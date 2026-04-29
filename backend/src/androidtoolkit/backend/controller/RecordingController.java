package androidtoolkit.backend.controller;

import androidtoolkit.app.AppServices;
import androidtoolkit.app.RecordingActionResponse;
import androidtoolkit.app.RecordingManager;
import androidtoolkit.domain.RecordingSession;
import androidtoolkit.service.ScreenRecordingService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static androidtoolkit.backend.validation.InputValidator.validateSerial;

@RestController
@RequestMapping("/api/devices")
public class RecordingController {

    private final RecordingManager recordingManager;
    private final ScreenRecordingService screenRecordingService;
    private final Map<String, RecordingSession> recordingSessions = new ConcurrentHashMap<>();

    public RecordingController(RecordingManager recordingManager, AppServices appServices) {
        this.recordingManager = recordingManager;
        this.screenRecordingService = appServices.screenRecordingService();
    }

    @PostMapping("/{serial}/screen-mirror")
    public Map<String, Object> startScreenMirror(@PathVariable String serial) {
        validateSerial(serial);
        try {
            screenRecordingService.startScreenMirrorAsync(serial);
            return Map.of("success", true, "message", "Screen mirror started for " + serial);
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/{serial}/start-recording")
    public Map<String, Object> startRecording(@PathVariable String serial) {
        validateSerial(serial);
        RecordingSession session = recordingSessions.computeIfAbsent(serial, k -> new RecordingSession());
        if (session.isActive()) {
            return Map.of("success", false, "message", "Recording already in progress on " + serial);
        }
        try {
            RecordingActionResponse result = recordingManager.startRecording(serial, serial, session);
            return Map.of("success", result.isSuccess(), "message", result.getMessage());
        } catch (RuntimeException e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/{serial}/stop-recording")
    public Map<String, Object> stopRecording(@PathVariable String serial, @RequestBody(required = false) Map<String, String> body) {
        validateSerial(serial);
        RecordingSession session = recordingSessions.get(serial);
        if (session == null || !session.isActive()) {
            return Map.of("success", false, "message", "No active recording on " + serial);
        }
        String pid = (body != null) ? body.getOrDefault("pid", "") : "";
        String recordingFileName = session.getRecordingFileName();
        try {
            RecordingActionResponse result = recordingManager.stopRecording(serial, serial, pid, session);
            return Map.of(
                    "success", result.isSuccess(),
                    "message", result.getMessage(),
                    "recordingLocation", result.getRecordingLocation() != null ? result.getRecordingLocation() : "",
                    "recordingFileName", recordingFileName != null ? recordingFileName : ""
            );
        } catch (Exception e) {
            session.setRecordingProcess(null);
            session.getRecordingInProgress().set(false);
            return Map.of("success", false, "message", "Stop recording failed: " + e.getMessage());
        }
    }
}
