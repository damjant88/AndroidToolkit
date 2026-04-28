package androidtoolkit.ui;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.File;

public class MyFrameUiSupport {

    public JMenuBar createMenuBar(JMenuItem consoleViewMenu, ActionListener defaultBuildLocationAction, ActionListener consoleToggleAction) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu helpMenu = new JMenu("Help");
        JMenu toolsMenu = new JMenu("Tools");
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        menuBar.add(toolsMenu);

        JMenuItem defaultBuildLocation = new JMenuItem("Set Default 'Select Build' Location");
        editMenu.add(defaultBuildLocation);
        defaultBuildLocation.addActionListener(defaultBuildLocationAction);

        toolsMenu.add(consoleViewMenu);
        consoleViewMenu.addActionListener(consoleToggleAction);
        return menuBar;
    }

    public void configureFrame(JFrame frame, int baseHeight, Image iconImage) {
        frame.setTitle("Adb Toolkit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setMinimumSize(new Dimension(650, baseHeight));
        frame.setIconImage(iconImage);
    }

    public File chooseBuildFile(Component parent, File defaultLocation, Component anchor) {
        JFileChooser fileChooser = new JFileChooser(defaultLocation.getAbsolutePath());
        int response = fileChooser.showOpenDialog(anchor);
        if (response == JFileChooser.APPROVE_OPTION) {
            return new File(fileChooser.getSelectedFile().getAbsolutePath());
        }
        return null;
    }

    public File chooseDirectory(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int response = fileChooser.showOpenDialog(parent);
        if (response == JFileChooser.APPROVE_OPTION) {
            return new File(fileChooser.getSelectedFile().getAbsolutePath());
        }
        return null;
    }

    public void showInfoMessage(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
