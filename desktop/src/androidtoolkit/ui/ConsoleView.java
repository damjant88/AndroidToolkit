package androidtoolkit.ui;

import androidtoolkit.service.CommandExecutor;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import androidtoolkit.app.BuildOutputLog;

public class ConsoleView extends JPanel implements BuildOutputLog {
    private final CommandExecutor commandExecutor;
    JTextArea textArea;
    private JScrollPane scrollPane;
    private JTextField commandField;

    public ConsoleView(MyFrame frame, CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.setBounds(0, 400, 630, 200);
        this.setBorder(new EtchedBorder());
        this.setBorder(BorderFactory.createEtchedBorder());
        this.setVisible(false);
        this.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new BorderLayout());

        commandField = new JTextField();
        commandField.setBackground(Color.WHITE);
        inputPanel.add(commandField, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setBorder(BorderFactory.createEtchedBorder());
        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        inputPanel.add(scrollPane, BorderLayout.CENTER);

        this.add(inputPanel, BorderLayout.CENTER);

        commandField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeCommand();
                    commandField.setText("");
                }
            }
        });
    }

    private void executeCommand() {
        try {
            String command = commandField.getText();
            if (command.equals("clear")) {
                textArea.setText("");
            } else {
                String output = commandExecutor.runCommand(command);
                appendText(output);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            appendText("Command not valid!");
        }
    }

    public void appendText(String text) {
        textArea.append(text + "\n\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}
