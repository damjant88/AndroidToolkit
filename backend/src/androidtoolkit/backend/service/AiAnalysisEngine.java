package androidtoolkit.backend.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for AI-driven log analysis.
 * Implementations may use OpenAI, a local LLM, or any other AI backend
 * to analyze log content and produce structured analysis results.
 */
public interface AiAnalysisEngine {

    /**
     * Analyzes a collection of log content grouped by device.
     *
     * @param projectName the name of the project whose logs are being analyzed
     * @param logContentByDevice a map from device serial to the list of log file contents
     *                           extracted from that device's archives
     * @return a structured analysis result containing log structure patterns,
     *         errors found, improvement suggestions, and risk assessment
     */
    AnalysisResult analyze(String projectName, Map<String, List<String>> logContentByDevice);
}
