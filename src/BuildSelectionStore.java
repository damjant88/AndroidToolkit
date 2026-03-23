import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class BuildSelectionStore {

    private final StoragePaths storagePaths;

    public BuildSelectionStore(StoragePaths storagePaths) {
        this.storagePaths = storagePaths;
    }

    public BuildSelectionState loadBuildSelection() {
        File buildsFile = storagePaths.buildsFile();
        if (!buildsFile.exists()) {
            return new BuildSelectionState(new ArrayList<>());
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(buildsFile))) {
            ArrayList<String> builds = (ArrayList<String>) objectInputStream.readObject();
            saveBuildSelection(new BuildSelectionState(builds));
            return new BuildSelectionState(builds);
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveBuildSelection(BuildSelectionState buildSelectionState) {
        storagePaths.rootDir().mkdirs();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(storagePaths.buildsFile()))) {
            objectOutputStream.writeObject(buildSelectionState.getBuildPaths());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File loadDefaultBuildLocation() {
        File locationFile = storagePaths.locationFile();
        if (!locationFile.exists()) {
            return locationFile;
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(locationFile))) {
            return (File) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveDefaultBuildLocation(File location) {
        storagePaths.rootDir().mkdirs();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(storagePaths.locationFile()))) {
            objectOutputStream.writeObject(location);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
