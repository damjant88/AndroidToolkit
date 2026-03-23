import java.util.ArrayList;
import java.util.List;

public class BuildSelectionState {

    private final ArrayList<String> buildPaths;

    public BuildSelectionState(List<String> buildPaths) {
        this.buildPaths = new ArrayList<>(buildPaths);
    }

    public ArrayList<String> getBuildPaths() {
        return new ArrayList<>(buildPaths);
    }

    public ArrayList<String> getBuildNames() {
        ArrayList<String> names = new ArrayList<>();
        for (String buildPath : buildPaths) {
            if (buildPath != null) {
                names.add(extractBuildName(buildPath));
            }
        }
        return names;
    }

    public boolean hasBuilds() {
        return !buildPaths.isEmpty();
    }

    public String getPrimaryBuildPath() {
        return hasBuilds() ? buildPaths.get(0) : "";
    }

    public String getPrimaryBuildName() {
        return hasBuilds() ? extractBuildName(buildPaths.get(0)) : "";
    }

    public void addBuild(String buildPath) {
        int existingIndex = buildPaths.indexOf(buildPath);
        if (existingIndex >= 0) {
            buildPaths.remove(existingIndex);
        }
        buildPaths.add(0, buildPath);
        while (buildPaths.size() > 5) {
            buildPaths.remove(5);
        }
    }

    public void selectBuild(int index) {
        if (index < 0 || index >= buildPaths.size() || index == 0) {
            return;
        }
        String selectedBuild = buildPaths.remove(index);
        buildPaths.add(0, selectedBuild);
    }

    private String extractBuildName(String buildPath) {
        String separator = buildPath.contains("/") ? "/" : "\\";
        return buildPath.substring(buildPath.lastIndexOf(separator) + 1);
    }
}
