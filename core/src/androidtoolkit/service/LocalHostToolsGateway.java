package androidtoolkit.service;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class LocalHostToolsGateway implements HostToolsGateway {

    @Override
    public void openFolder(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Folder does not exist or is not a directory: " + folder.getPath());
        }
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public File findScrcpyExecutable() {
        String[] pathDirectories = System.getenv("PATH").split(File.pathSeparator);
        for (String directory : pathDirectories) {
            File scrcpyExecutable = new File(directory, "scrcpy.exe");
            if (scrcpyExecutable.exists()) {
                return scrcpyExecutable;
            }
        }
        return null;
    }
}
