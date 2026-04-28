package androidtoolkit.ui;

import androidtoolkit.domain.BuildSelectionState;
import androidtoolkit.service.BuildSelectionStore;

import java.io.File;

public class BuildSelectionCoordinator {

    private final BuildSelectionStore buildSelectionStore;
    private BuildSelectionState buildSelectionState;

    public BuildSelectionCoordinator(BuildSelectionStore buildSelectionStore) {
        this.buildSelectionStore = buildSelectionStore;
        this.buildSelectionState = new BuildSelectionState(java.util.List.of());
    }

    public BuildSelectionState loadInitialState() {
        buildSelectionState = buildSelectionStore.loadBuildSelection();
        return buildSelectionState;
    }

    public BuildSelectionState currentState() {
        return buildSelectionState;
    }

    public void chooseBuild(BuildSelectionUi ui) {
        File defaultLocation = buildSelectionStore.loadDefaultBuildLocation();
        File selectedBuild = ui.chooseBuildFile(defaultLocation);
        if (selectedBuild == null) {
            return;
        }

        buildSelectionState.addBuild(selectedBuild.getAbsolutePath());
        refreshBuildSelection(ui);
        ui.selectBuildIndex(0);
        buildSelectionStore.saveBuildSelection(buildSelectionState);
        ui.onBuildSelectionChanged();
    }

    public void selectBuildAt(int index, BuildSelectionUi ui) {
        buildSelectionState.selectBuild(index);
        refreshBuildSelection(ui);
        buildSelectionStore.saveBuildSelection(buildSelectionState);
    }

    public void chooseDefaultBuildLocation(BuildSelectionUi ui) {
        File selectedLocation = ui.chooseDefaultBuildLocation();
        if (selectedLocation == null) {
            return;
        }

        buildSelectionStore.saveDefaultBuildLocation(selectedLocation);
        ui.showDefaultBuildLocationSaved(selectedLocation);
    }

    private void refreshBuildSelection(BuildSelectionUi ui) {
        ui.refreshBuildNames(buildSelectionState.getBuildNames());
    }

    public interface BuildSelectionUi {
        File chooseBuildFile(File defaultLocation);
        File chooseDefaultBuildLocation();
        void refreshBuildNames(java.util.List<String> buildNames);
        void selectBuildIndex(int index);
        void onBuildSelectionChanged();
        void showDefaultBuildLocationSaved(File location);
    }
}
