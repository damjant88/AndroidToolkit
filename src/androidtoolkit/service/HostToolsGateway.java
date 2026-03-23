package androidtoolkit.service;

import java.io.File;

public interface HostToolsGateway {

    void openFolder(File folder);

    File findScrcpyExecutable();
}
