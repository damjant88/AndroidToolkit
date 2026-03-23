import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandExecutor {

    private volatile boolean stopRequested = false;

    public String runCommand(String command) {
        return runCommand(tokenize(command));
    }

    public String runCommand(List<String> commandParts) {
        try {
            ProcessBuilder pb = new ProcessBuilder(commandParts).redirectErrorStream(true);
            Process process = pb.start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .collect(Collectors.joining("\r\n"));
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
            return output.trim();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public String runLiveLogs(String command, String searchString) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder pb = new ProcessBuilder(tokenize(command)).redirectErrorStream(true);
            Process process = pb.start();
            stopRequested = false;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while (!stopRequested && (line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                    if (line.contains(searchString)) {
                        System.out.println(line);
                    }
                }
            }

            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return output.toString().trim();
    }

    public void stopTracking() {
        stopRequested = true;
    }

    public void runCommandAndSave(String command, String fileName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(tokenize(command));
            processBuilder.redirectOutput(new File(fileName));
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private List<String> tokenize(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char ch = command.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
