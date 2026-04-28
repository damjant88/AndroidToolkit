package androidtoolkit.ui;

import javax.swing.JMenuItem;

public class MyFrameInitializer {

    public void initialize(MyFrame frame, MyFrameComponents components, MyFrameUiSupport uiSupport, Icons icon, int baseHeight) {
        JMenuItem consoleViewMenu = components.consoleViewMenu();
        frame.setJMenuBar(uiSupport.createMenuBar(
                consoleViewMenu,
                new DefaultBuildLocationAction(frame),
                new ConsoleViewToggleAction(frame)
        ));

        components.installButton().addActionListener(new InstallSelectedDevicesAction(frame));
        frame.add(components.installButton());

        components.uninstallAllButton().addActionListener(new UninstallAllDevicesAction(frame));
        frame.add(components.uninstallAllButton());

        frame.add(components.staticPane());

        components.fileTextFieldBox().addActionListener(new SelectBuildAction(frame));
        frame.add(components.fileTextFieldBox());

        components.refreshDevicesButton().addActionListener(new RefreshDevicesAction(frame));
        frame.add(components.refreshDevicesButton());

        components.fileButton().addActionListener(new ChooseBuildAction(frame));
        frame.add(components.fileButton());

        frame.add(components.progressBar());
        frame.add(components.consoleView());

        uiSupport.configureFrame(frame, baseHeight, icon.frameIcon.getImage());
    }
}
