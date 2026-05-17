package androidtoolkit.backend.device;

import androidtoolkit.backend.service.AgentConnectionManager;
import net.jqwik.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for {@link LogcatRelayService} logcat line truncation.
 *
 * <p>Validates: Requirements 5.6
 */
@Tag("Feature: agent-device-relay, Property 11: Logcat line truncation")
class LogcatRelayPropertyTest {

    private static final int MAX_LINE_LENGTH = 4096;

    /**
     * Property 11: Logcat line truncation
     * For any string of any length, the forwarded line is at most 4096 characters.
     * Lines exceeding this limit are truncated to exactly 4096 characters.
     */
    @Property(tries = 100)
    void forwardedLineIsAtMost4096Characters(
            @ForAll("logcatLines") String line) {

        // Arrange
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AgentConnectionManager connectionManager = mock(AgentConnectionManager.class);
        LogcatRelayService service = new LogcatRelayService(connectionManager, messagingTemplate);

        String serial = "device-001";
        long timestamp = System.currentTimeMillis();

        // Act
        service.forwardLine(serial, line, timestamp);

        // Assert - capture the message sent to the template
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> messageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/logcat/" + serial), messageCaptor.capture());

        Map<String, Object> sentMessage = messageCaptor.getValue();
        String sentLine = (String) sentMessage.get("line");

        // The forwarded line must be at most 4096 characters
        assertTrue(sentLine.length() <= MAX_LINE_LENGTH,
                "Forwarded line length " + sentLine.length() + " exceeds maximum of " + MAX_LINE_LENGTH);

        // If the original line was longer than 4096, it must be truncated to exactly 4096
        if (line.length() > MAX_LINE_LENGTH) {
            assertEquals(MAX_LINE_LENGTH, sentLine.length(),
                    "Lines exceeding " + MAX_LINE_LENGTH + " chars must be truncated to exactly " + MAX_LINE_LENGTH);
            assertEquals(line.substring(0, MAX_LINE_LENGTH), sentLine,
                    "Truncated line must be the first 4096 characters of the original");
        } else {
            // If the original line was within limits, it should be forwarded unchanged
            assertEquals(line, sentLine,
                    "Lines within the limit should be forwarded unchanged");
        }
    }

    @Provide
    Arbitrary<String> logcatLines() {
        return Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(10000)
                .ascii();
    }
}
