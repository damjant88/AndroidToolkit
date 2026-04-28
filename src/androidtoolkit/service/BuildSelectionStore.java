package androidtoolkit.service;

import androidtoolkit.domain.BuildSelectionState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class BuildSelectionStore {

    private final StorageService storageService;

    public BuildSelectionStore(StorageService storageService) {
        this.storageService = storageService;
    }

    public BuildSelectionState loadBuildSelection() {
        File buildsFile = storageService.buildsFile();
        if (!buildsFile.exists()) {
            return new BuildSelectionState(new ArrayList<>());
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(buildsFile))) {
            ArrayList<String> builds = (ArrayList<String>) objectInputStream.readObject();
            return new BuildSelectionState(builds);
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveBuildSelection(BuildSelectionState buildSelectionState) {
        storageService.ensureDirectoryExists(storageService.rootDir());
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(storageService.buildsFile()))) {
            objectOutputStream.writeObject(buildSelectionState.getBuildPaths());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public File loadDefaultBuildLocation() {
        File locationFile = storageService.locationFile();
        if (!locationFile.exists()) {
            // No saved location — use user's home directory as a sensible default
            return new File(System.getProperty("user.home"));
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(locationFile))) {
            return (File) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveDefaultBuildLocation(File location) {
        storageService.ensureDirectoryExists(storageService.rootDir());
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(storageService.locationFile()))) {
            objectOutputStream.writeObject(location);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
