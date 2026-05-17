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
            case AgentCommand.Reboot cmd -> executor.submit(() -> rebootDevice(cmd));
            case AgentCommand.ToggleWifiDebug cmd -> executor.submit(() -> toggleWifiDebug(cmd));
            case AgentCommand.EnableFirebaseDebug cmd -> executor.submit(() -> enableFirebaseDebug(cmd));
            case AgentCommand.GetLocation cmd -> executor.submit(() -> getLocation(cmd));
            case AgentCommand.SetMockLocation cmd -> executor.submit(() -> setMockLocation(cmd));
            case AgentCommand.ManagePermission cmd -> executor.submit(() -> managePermission(cmd));
            case AgentCommand.ManageAccessibility cmd -> executor.submit(() -> manageAccessibility(cmd));
            case AgentCommand.StartScreenMirror cmd -> executor.submit(() -> startScreenMirror(cmd));
            case AgentCommand.GetPackageDump cmd -> executor.submit(() -> getPackageDump(cmd));
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
            // Capture screenshot directly to stdout as PNG bytes (no temp file needed)
            ProcessBuilder screencap = new ProcessBuilder("adb", "-s", cmd.serial(), "exec-out", "screencap", "-p");
            Process process = screencap.start();
            byte[] imageBytes = process.getInputStream().readAllBytes();
            process.waitFor();

            if (imageBytes.length > 0) {
                // Send as base64 so the backend can serve it directly
                String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                serverConnection.send(new AgentMessage.OperationResult("screenshot-" + cmd.serial(),
                        true, "data:image/png;base64," + base64));
            } else {
                serverConnection.send(new AgentMessage.OperationResult("screenshot-" + cmd.serial(),
                        false, "Screenshot capture returned empty data"));
            }
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("screenshot-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void rebootDevice(AgentCommand.Reboot cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "reboot");
            Process process = pb.start();
            int exitCode = process.waitFor();
            serverConnection.send(new AgentMessage.OperationResult("reboot-" + cmd.serial(),
                    exitCode == 0, exitCode == 0 ? "Rebooted" : "Exit code: " + exitCode));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("reboot-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void toggleWifiDebug(AgentCommand.ToggleWifiDebug cmd) {
        try {
            String requestId = "wifi-debug-" + cmd.serial();
            if (cmd.wifiDebugSession()) {
                // Stop wifi debugging
                runAdb(cmd.serial(), "disconnect", cmd.ipAddress() + ":5555");
                serverConnection.send(new AgentMessage.OperationResult(requestId, true,
                        "DISCONNECTED:" + cmd.ipAddress()));
            } else if (cmd.hasWifiIp()) {
                // Start wifi debugging
                runAdb(cmd.serial(), "tcpip", "5555");
                Thread.sleep(1000);
                String result = runAdbDirect("connect", cmd.ipAddress() + ":5555");
                boolean success = result.contains("connected");
                serverConnection.send(new AgentMessage.OperationResult(requestId, success,
                        success ? "CONNECTED:" + cmd.ipAddress() : "FAILED:" + result));
            } else {
                serverConnection.send(new AgentMessage.OperationResult(requestId, false,
                        "NO_WIFI_IP"));
            }
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("wifi-debug-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void enableFirebaseDebug(AgentCommand.EnableFirebaseDebug cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell",
                    "setprop", "debug.firebase.analytics.app", cmd.packageName());
            int exitCode = pb.start().waitFor();
            serverConnection.send(new AgentMessage.OperationResult("firebase-" + cmd.serial(),
                    exitCode == 0, exitCode == 0 ? "Firebase debug enabled for " + cmd.packageName() : "Exit code: " + exitCode));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("firebase-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void getLocation(AgentCommand.GetLocation cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell", "dumpsys", "location");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            double lat = 0, lng = 0;
            boolean found = false;
            for (String line : output.split("\n")) {
                if (line.contains("last location=") && line.contains("Location[")) {
                    try {
                        int idx = line.indexOf("Location[");
                        String sub = line.substring(idx);
                        int start = sub.indexOf(' ') + 1;
                        int comma = sub.indexOf(',', start);
                        int end = sub.indexOf(' ', comma);
                        if (end == -1) end = sub.indexOf(']', comma);
                        lat = Double.parseDouble(sub.substring(start, comma));
                        lng = Double.parseDouble(sub.substring(comma + 1, end));
                        found = true;
                        break;
                    } catch (Exception ignored) {}
                }
            }
            String detail = found ? String.format("%.6f,%.6f", lat, lng) : "NOT_FOUND";
            serverConnection.send(new AgentMessage.OperationResult("location-" + cmd.serial(), found, detail));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("location-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void setMockLocation(AgentCommand.SetMockLocation cmd) {
        try {
            String requestId = "mock-location-" + cmd.serial();
            if (!cmd.start()) {
                runAdb(cmd.serial(), "shell", "cmd location providers remove-test-provider gps");
                serverConnection.send(new AgentMessage.OperationResult(requestId, true, "Mock location stopped"));
                return;
            }
            runAdb(cmd.serial(), "shell", "appops set com.android.shell android:mock_location allow");
            runAdb(cmd.serial(), "shell", "cmd location providers add-test-provider gps");
            runAdb(cmd.serial(), "shell", "cmd location providers set-test-provider-enabled gps true");
            String locCmd = String.format(
                    "cmd location providers set-test-provider-location gps --location %f,%f --accuracy 1.0",
                    cmd.lat(), cmd.lng());
            runAdb(cmd.serial(), "shell", locCmd);
            serverConnection.send(new AgentMessage.OperationResult(requestId, true,
                    String.format("Mocking: %.6f,%.6f", cmd.lat(), cmd.lng())));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("mock-location-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void managePermission(AgentCommand.ManagePermission cmd) {
        try {
            String requestId = "permission-" + cmd.serial();
            String shellCmd = switch (cmd.action()) {
                case "grant" -> "pm grant " + cmd.packageName() + " " + cmd.permission();
                case "revoke" -> "pm revoke " + cmd.packageName() + " " + cmd.permission();
                case "addIdleWhitelist" -> "dumpsys deviceidle whitelist +" + cmd.packageName();
                case "removeIdleWhitelist" -> "dumpsys deviceidle whitelist -" + cmd.packageName();
                case "ignoreAutoRevoke" -> "cmd appops set " + cmd.packageName() + " AUTO_REVOKE_PERMISSIONS_IF_UNUSED ignore";
                case "resetAutoRevoke" -> "cmd appops set " + cmd.packageName() + " AUTO_REVOKE_PERMISSIONS_IF_UNUSED default";
                default -> throw new IllegalArgumentException("Unknown action: " + cmd.action());
            };
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell", shellCmd);
            int exitCode = pb.start().waitFor();
            serverConnection.send(new AgentMessage.OperationResult(requestId, exitCode == 0,
                    exitCode == 0 ? cmd.action() + " completed" : "Exit code: " + exitCode));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("permission-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void manageAccessibility(AgentCommand.ManageAccessibility cmd) {
        try {
            String requestId = "accessibility-" + cmd.serial();
            String service = cmd.packageName() + "/" + cmd.serviceClassName();
            // Get current enabled services
            ProcessBuilder getPb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell",
                    "settings", "get", "secure", "enabled_accessibility_services");
            getPb.redirectErrorStream(true);
            Process getProcess = getPb.start();
            String current = new String(getProcess.getInputStream().readAllBytes()).trim();
            getProcess.waitFor();

            String newValue;
            if (cmd.enable()) {
                newValue = current.isEmpty() || "null".equals(current) ? service : current + ":" + service;
            } else {
                newValue = current.replace(service, "").replace("::", ":").replaceAll("^:|:$", "");
            }

            ProcessBuilder setPb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell",
                    "settings", "put", "secure", "enabled_accessibility_services", newValue);
            int exitCode = setPb.start().waitFor();
            serverConnection.send(new AgentMessage.OperationResult(requestId, exitCode == 0,
                    (cmd.enable() ? "Enabled" : "Disabled") + " " + cmd.serviceClassName()));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("accessibility-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private void startScreenMirror(AgentCommand.StartScreenMirror cmd) {
        try {
            // Use scrcpy if available, otherwise report not supported
            ProcessBuilder pb = new ProcessBuilder("scrcpy", "-s", cmd.serial(), "--no-audio");
            pb.start();
            serverConnection.send(new AgentMessage.OperationResult("screen-mirror-" + cmd.serial(), true, "Screen mirror started"));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("screen-mirror-" + cmd.serial(), false,
                    "Screen mirror failed (scrcpy required): " + e.getMessage()));
        }
    }

    private void getPackageDump(AgentCommand.GetPackageDump cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", cmd.serial(), "shell",
                    "dumpsys", "package", cmd.packageName());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            // Truncate if too long (max 64KB for WebSocket message)
            if (output.length() > 65000) {
                output = output.substring(0, 65000) + "\n... [truncated]";
            }
            serverConnection.send(new AgentMessage.OperationResult("package-dump-" + cmd.serial(), true, output));
        } catch (Exception e) {
            serverConnection.send(new AgentMessage.OperationResult("package-dump-" + cmd.serial(), false, e.getMessage()));
        }
    }

    private String runAdb(String serial, String... args) throws Exception {
        String[] cmd = new String[args.length + 3];
        cmd[0] = "adb";
        cmd[1] = "-s";
        cmd[2] = serial;
        System.arraycopy(args, 0, cmd, 3, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();
        return output;
    }

    private String runAdbDirect(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "adb";
        System.arraycopy(args, 0, cmd, 1, args.length);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();
        return output;
    }
}
