package androidtoolkit.agent.adb;

import androidtoolkit.agent.connection.ServerConnection;
import androidtoolkit.domain.agent.AgentCommand;
import androidtoolkit.domain.agent.AgentMessage;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes ADB commands on locally connected devices in response to Server commands.
 */
@Component
public class AdbCommandExecutor {

    private final ServerConnection serverConnection;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, Process> activeLogcatProcesses = new ConcurrentHashMap<>();

    public AdbCommandExecutor(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    @PostConstruct
    public void init() {
        serverConnection.setCommandHandler(this::handleCommand);
    }

    public void handleCommand(AgentCommand command) {
        switch (command) {
            case AgentCommand.StartLogcat cmd -> startLogcat(cmd);
            case AgentCommand.StopLogcat cmd -> stopLogcat(cmd);
            case AgentCommand.PullLogs cmd -> executor.submit(() -> pullLogs(cmd));
            case AgentCommand.InstallApk cmd -> executor.submit(() -> installApk(cmd));
            case AgentCommand.UninstallApp cmd -> executor.submit(() -> uninstallApp(cmd));
            case AgentCommand.CaptureScreenshot cmd -> executor.submit(() -> captureScreenshot(cmd));
        }
    }

    private void startLogcat(AgentCommand.StartLogcat cmd) {
        executor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "logcat");
                Process process = pb.start();
                activeLogcatProcesses.put(cmd.serial(), process);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        serverConnection.send(new AgentMessage.LogcatLine(cmd.serial(), line, System.currentTimeMillis()));
                    }
                }
            } catch (Exception e) {
                serverConnection.send(new AgentMessage.OperationResult(cmd.requestId(), false, e.getMessage()));
            } finally {
                activeLogcatProcesses.remove(cmd.serial());
            }
        });
    }

    private void stopLogcat(AgentCommand.StopLogcat cmd) {
        Process process = activeLogcatProcesses.remove(cmd.serial());
        if (process != null) {
            process.destroyForcibly();
        }
    }

    private void pullLogs(AgentCommand.PullLogs cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "pull", "/sdcard/logs", cmd.targetPath());
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                serverConnection.send(new AgentMessage.LogArchiveReady(cmd.serial(), cmd.targetPath()));
            } else {
                serverConnection.send(new AgentMessage.OperationResult("pull-" + cmd.serial(), false, "Exit code: " + exitCode));
            }
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("pull-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void installApk(AgentCommand.InstallApk cmd) {
        try {
            // Download APK from URL first, then install
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "install", "-r", cmd.apkUrl());
            Process process = pb.start();
            int exitCode = process.waitFor();
            serverConnection.send(new AgentMessage.OperationResult("install-" + cmd.serial(),
                    exitCode == 0, exitCode == 0 ? "Installed" : "Exit code: " + exitCode));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("install-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void uninstallApp(AgentCommand.UninstallApp cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "uninstall", cmd.packageName());
            Process process = pb.start();
            int exitCode = process.waitFor();
            serverConnection.send(new AgentMessage.OperationResult("uninstall-" + cmd.serial(),
                    exitCode == 0, exitCode == 0 ? "Uninstalled" : "Exit code: " + exitCode));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("uninstall-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void captureScreenshot(AgentCommand.CaptureScreenshot cmd) {
        try {
            String remotePath = "/sdcard/screenshot_" + System.currentTimeMillis() + ".png";
            ProcessBuilder screencap = new ProcessBuilder("adb", "-s", cmd.serial(), "shell", "screencap", "-p", remotePath);
            screencap.start().waitFor();

            String localPath = "screenshots/" + cmd.serial() + "_" + System.currentTimeMillis() + ".png";
            ProcessBuilder pull = new ProcessBuilder("adb", "-s", cmd.serial(), "pull", remotePath, localPath);
            int exitCode = pull.start().waitFor();

            serverConnection.send(new AgentMessage.OperationResult("screenshot-" + cmd.serial(),
                    exitCode == 0, exitCode == 0 ? localPath : "Failed"));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("screenshot-" + cmd.serial(), false, e.getMessage()));
        }
    }
}
