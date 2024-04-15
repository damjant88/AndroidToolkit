import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiveEventTracker extends JFrame {
    private JTextField eventTextField;
    private JButton trackButton;
    private JTextArea logTextArea;
    Util utility = new Util();
    String serial;
    String pid;
    String event;

    public LiveEventTracker(String serial, String pid) {
        this.serial = serial;
        this.pid = pid;
        event = "Adjust";
        setTitle("Live Event Tracker");
        setSize(500, 400);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
                TrackEventWorker worker = new TrackEventWorker();
                if (trackButton.getText().equals("Track")) {
                    trackButton.setText("Stop Tracking");
                    event = eventTextField.getText();
                    worker.execute();
                } else {
                    worker.stopTracking();
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
            output = utility.runLiveLogs(command, event);
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
            utility.stopTracking(); // Call the stopTracking method of RunLiveLogs
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

