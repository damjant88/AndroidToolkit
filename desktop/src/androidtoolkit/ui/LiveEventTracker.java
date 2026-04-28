package androidtoolkit.ui;

import androidtoolkit.service.CommandExecutor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveEventTracker extends JFrame {
    private JTextField eventTextField;
    private JButton trackButton;
    private JTextArea logTextArea;
    private final CommandExecutor commandExecutor;
    String serial;
    String pid;
    String event;
    // Keep a reference to the running worker so we can stop the correct one
    TrackEventWorker activeWorker;
    // Each tracker owns its own stop flag so stopping one doesn't affect others
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    public LiveEventTracker(String serial, String pid, CommandExecutor commandExecutor) {
        this.serial = serial;
        this.pid = pid;
        this.commandExecutor = commandExecutor;
        event = "Adjust";
        setTitle("Live Event Tracker");
        setSize(500, 400);
        setVisible(true);
        // DISPOSE_ON_CLOSE only closes this window. EXIT_ON_CLOSE would kill the entire app.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Initialize components
        eventTextField = new JTextField();
        trackButton = new JButton("Track");
        trackButton.setEnabled(true);
        logTextArea = new JTextArea();

        // Set layout
        setLayout(new BorderLayout());

        // Add components to the frame
        add(eventTextField, BorderLayout.NORTH);
        add(trackButton, BorderLayout.CENTER);
        add(new JScrollPane(logTextArea), BorderLayout.SOUTH);

        // Action listener for the 'Track' button
        trackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (trackButton.getText().equals("Track")) {
                    event = eventTextField.getText();
                    // Create and remember the worker so we can stop it later
                    activeWorker = new TrackEventWorker();
                    trackButton.setText("Stop Tracking");
                    activeWorker.execute();
                } else {
                    // Stop the actual running worker, not a new one
                    if (activeWorker != null) {
                        activeWorker.stopTracking();
                        activeWorker = null;
                    }
                    trackButton.setText("Track");
                }
            }
        });
    }

    class TrackEventWorker extends SwingWorker<Void, Void> {
        private String output;

        @Override
        protected Void doInBackground() throws Exception {
            String command = "adb -s " + serial + " logcat --pid=" + pid;
            stopFlag.set(false);
            output = commandExecutor.runLiveLogs(command, event, stopFlag);
            return null;
        }

        @Override
        protected void done() {
            // Display log lines containing the event
            logTextArea.setText(output);

            // Count occurrences of the event
            int count = countOccurrences(output, event);
        }

        public void stopTracking() {
            stopFlag.set(true);
        }
    }

    // Method to count occurrences of a string in another string
    private int countOccurrences(String text, String searchString) {
        int count = 0;
        Pattern pattern = Pattern.compile(searchString);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}

