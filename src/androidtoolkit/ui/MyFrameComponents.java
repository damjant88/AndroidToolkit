package androidtoolkit.ui;

import androidtoolkit.domain.BuildSelectionState;
import androidtoolkit.service.CommandExecutor;
import androidtoolkit.ui.components.FileButton;
import androidtoolkit.ui.components.InstallButton;
import androidtoolkit.ui.components.RefreshDevicesButton;
import androidtoolkit.ui.components.UninstallAllButton;

import javax.swing.JMenuItem;
import javax.swing.JTextPane;

public class MyFrameComponents {

    private final JMenuItem consoleViewMenu;
    private final RefreshDevicesButton refreshDevicesButton;
    private final FileButton fileButton;
    private final InstallButton installButton;
    private final UninstallAllButton uninstallAllButton;
    private final JTextPane staticPane;
    private final ProgressBar progressBar;
    private final FileTextFieldBox fileTextFieldBox;
    private final ConsoleView consoleView;

    public MyFrameComponents(MyFrame owner, Icons icon, BuildSelectionState buildSelectionState, CommandExecutor commandExecutor) {
        this.consoleViewMenu = new JMenuItem("Show Console View");
        this.refreshDevicesButton = new RefreshDevicesButton(icon.display_icon);
        this.fileButton = new FileButton("Select Build");
        this.installButton = new InstallButton();
        this.uninstallAllButton = new UninstallAllButton();
        this.staticPane = new StaticPane();
        this.progressBar = new ProgressBar();
        this.fileTextFieldBox = new FileTextFieldBox(buildSelectionState.getBuildNames());
        this.consoleView = new ConsoleView(owner, commandExecutor);
    }

    public JMenuItem consoleViewMenu() {
        return consoleViewMenu;
    }

    public RefreshDevicesButton refreshDevicesButton() {
        return refreshDevicesButton;
    }

    public FileButton fileButton() {
        return fileButton;
    }

    public InstallButton installButton() {
        return installButton;
    }

    public UninstallAllButton uninstallAllButton() {
        return uninstallAllButton;
    }

    public JTextPane staticPane() {
        return staticPane;
    }

    public ProgressBar progressBar() {
        return progressBar;
    }

    public FileTextFieldBox fileTextFieldBox() {
        return fileTextFieldBox;
    }

    public ConsoleView consoleView() {
        return consoleView;
    }
}
