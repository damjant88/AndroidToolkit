package androidtoolkit.ui;

import androidtoolkit.app.PermissionDefinition;
import androidtoolkit.app.PermissionUpdateResult;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PermissionsDialog extends JDialog {

    private final List<PermissionDefinition> definitions;
    private final List<JCheckBox> permissionBoxes = new ArrayList<>();
    private final List<JLabel> statusLabels = new ArrayList<>();
    private final JCheckBox selectAllBox = new JCheckBox("Select All", true);
    private final JButton enableButton = new JButton("Enable");
    private final JButton disableButton = new JButton("Disable");
    private static final Color ENABLED_COLOR = new Color(0, 128, 0);
    private static final Color DISABLED_COLOR = new Color(180, 0, 0);
    private static final Color UNAVAILABLE_COLOR = Color.GRAY;

    public PermissionsDialog(
            Frame owner,
            String deviceName,
            String packageName,
            List<PermissionDefinition> definitions,
            Set<String> activePermissionIds,
            Set<String> unavailablePermissionIds
    ) {
        super(owner, "Permissions", true);
        this.definitions = new ArrayList<>(definitions);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        JPanel infoPanel = new JPanel(new GridLayout(0, 1));
        infoPanel.add(new JLabel("Device: " + deviceName));
        infoPanel.add(new JLabel("Package: " + packageName));
        content.add(infoPanel, BorderLayout.NORTH);

        JPanel permissionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 6, 8);
        selectAllBox.addActionListener(event -> setAllSelections(selectAllBox.isSelected()));
        permissionsPanel.add(selectAllBox, constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.0;
        permissionsPanel.add(new JLabel("Status"), constraints);

        for (PermissionDefinition definition : this.definitions) {
            boolean unavailable = unavailablePermissionIds.contains(definition.getId());
            JCheckBox box = new JCheckBox(definition.getLabel(), activePermissionIds.contains(definition.getId()));
            box.setEnabled(!unavailable);
            box.addActionListener(event -> syncSelectAllState());
            permissionBoxes.add(box);

            JLabel statusLabel = createStatusLabel(box.isSelected(), unavailable);
            statusLabels.add(statusLabel);

            constraints.gridy++;
            constraints.gridx = 0;
            constraints.weightx = 1.0;
            permissionsPanel.add(box, constraints);

            constraints.gridx = 1;
            constraints.weightx = 0.0;
            permissionsPanel.add(statusLabel, constraints);
        }
        syncSelectAllState();

        JScrollPane scrollPane = new JScrollPane(permissionsPanel);
        content.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> dispose());
        buttonPanel.add(enableButton);
        buttonPanel.add(disableButton);
        buttonPanel.add(cancelButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(420, 380);
        setLocationRelativeTo(owner);
    }

    public void updateStatuses(Set<String> activePermissionIds, Set<String> unavailablePermissionIds) {
        for (int i = 0; i < definitions.size(); i++) {
            boolean unavailable = unavailablePermissionIds.contains(definitions.get(i).getId());
            boolean enabled = activePermissionIds.contains(definitions.get(i).getId());
            JCheckBox checkBox = permissionBoxes.get(i);
            checkBox.setEnabled(!unavailable);
            checkBox.setSelected(enabled && !unavailable);
            JLabel statusLabel = statusLabels.get(i);
            if (unavailable) {
                statusLabel.setText("Unavailable");
                statusLabel.setForeground(UNAVAILABLE_COLOR);
            } else {
                statusLabel.setText(enabled ? "Enabled" : "Disabled");
                statusLabel.setForeground(enabled ? ENABLED_COLOR : DISABLED_COLOR);
            }
        }
        syncSelectAllState();
    }

    public void setActionsEnabled(boolean enabled) {
        enableButton.setEnabled(enabled);
        disableButton.setEnabled(enabled);
    }

    public void setEnableAction(ActionListener actionListener) {
        for (ActionListener listener : enableButton.getActionListeners()) {
            enableButton.removeActionListener(listener);
        }
        enableButton.addActionListener(actionListener);
    }

    public void setDisableAction(ActionListener actionListener) {
        for (ActionListener listener : disableButton.getActionListeners()) {
            disableButton.removeActionListener(listener);
        }
        disableButton.addActionListener(actionListener);
    }

    public List<PermissionDefinition> selectedDefinitions() {
        List<PermissionDefinition> selected = new ArrayList<>();
        for (int i = 0; i < definitions.size(); i++) {
            JCheckBox checkBox = permissionBoxes.get(i);
            if (checkBox.isEnabled() && checkBox.isSelected()) {
                selected.add(definitions.get(i));
            }
        }
        return selected;
    }

    public void showResult(PermissionUpdateResult result, String deviceName) {
        JOptionPane.showMessageDialog(this, result.toDisplayMessage(deviceName),
                result.isSuccessful() ? "Permissions Updated" : "Permissions Updated With Issues",
                result.isSuccessful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void setAllSelections(boolean selected) {
        for (JCheckBox permissionBox : permissionBoxes) {
            if (permissionBox.isEnabled()) {
                permissionBox.setSelected(selected);
            }
        }
    }

    private void syncSelectAllState() {
        boolean allSelected = !permissionBoxes.isEmpty();
        for (JCheckBox permissionBox : permissionBoxes) {
            // Skip disabled (unavailable) checkboxes — they shouldn't affect "Select All"
            if (!permissionBox.isEnabled()) {
                continue;
            }
            if (!permissionBox.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAllBox.setSelected(allSelected);
    }

    private JLabel createStatusLabel(boolean enabled, boolean unavailable) {
        JLabel label = new JLabel(unavailable ? "Unavailable" : enabled ? "Enabled" : "Disabled");
        label.setForeground(unavailable ? UNAVAILABLE_COLOR : enabled ? ENABLED_COLOR : DISABLED_COLOR);
        return label;
    }
}
