package androidtoolkit.ui;

import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionUpdateResult;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class PermissionsDialog extends JDialog {

    private final List<PermissionDefinition> definitions;
    private final List<JCheckBox> permissionBoxes = new ArrayList<>();
    private final JCheckBox selectAllBox = new JCheckBox("Select All", true);
    private final JButton applyButton = new JButton("Apply");

    public PermissionsDialog(
            Frame owner,
            String deviceName,
            String packageName,
            List<PermissionDefinition> definitions
    ) {
        super(owner, "Permissions", true);
        this.definitions = new ArrayList<>(definitions);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        JPanel infoPanel = new JPanel(new GridLayout(0, 1));
        infoPanel.add(new JLabel("Device: " + deviceName));
        infoPanel.add(new JLabel("Package: " + packageName));
        content.add(infoPanel, BorderLayout.NORTH);

        JPanel permissionsPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        selectAllBox.addActionListener(event -> setAllSelections(selectAllBox.isSelected()));
        permissionsPanel.add(selectAllBox);

        for (PermissionDefinition definition : this.definitions) {
            JCheckBox box = new JCheckBox(definition.getLabel(), true);
            box.addActionListener(event -> syncSelectAllState());
            permissionBoxes.add(box);
            permissionsPanel.add(box);
        }

        JScrollPane scrollPane = new JScrollPane(permissionsPanel);
        content.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(420, 380);
        setLocationRelativeTo(owner);
    }

    public void setApplyEnabled(boolean enabled) {
        applyButton.setEnabled(enabled);
    }

    public void setApplyAction(ActionListener actionListener) {
        for (ActionListener listener : applyButton.getActionListeners()) {
            applyButton.removeActionListener(listener);
        }
        applyButton.addActionListener(actionListener);
    }

    public List<PermissionDefinition> selectedDefinitions() {
        List<PermissionDefinition> selected = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            if (permissionBoxes.get(i).isSelected()) {
                selected.add(definitions.get(i));
            }
        }
        return selected;
    }

    public void showResult(PermissionUpdateResult result, String deviceName) {
        JOptionPane.showMessageDialog(this, result.toDisplayMessage(deviceName),
                result.isSuccessful() ? "Permissions Applied" : "Permissions Applied With Issues",
                result.isSuccessful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void setAllSelections(boolean selected) {
        for (JCheckBox permissionBox : permissionBoxes) {
            permissionBox.setSelected(selected);
        }
    }

    private void syncSelectAllState() {
        boolean allSelected = !permissionBoxes.isEmpty();
        for (JCheckBox permissionBox : permissionBoxes) {
            if (!permissionBox.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAllBox.setSelected(allSelected);
    }
}
