import java.util.ArrayList;
import java.util.List;

public class PackageClassifier {

    private static final List<String> PACKAGE_HINTS = List.of(
            "safepath.family",
            "securefamily",
            "wavemarket",
            "safeandfound",
            "safefound",
            "familycontrols",
            "orangespain",
            "TuYo",
            "safepath.dish",
            "familymode"
    );

    private static final List<String> SUPPORTED_PACKAGES = List.of(
            "com.smithmicro.tmobile.familymode.test",
            "com.smithmicro.att.securefamily",
            "com.att.securefamilycompanion",
            "com.wavemarket.waplauncher",
            "com.smithmicro.safepath.family",
            "com.smithmicro.sprint.safeandfound.test",
            "com.sprint.safefound",
            "com.tmobile.familycontrols",
            "com.smithmicro.orangespain.test",
            "com.orange.es.TuYo",
            "com.smithmicro.safepath.dish.test",
            "com.smithmicro.safepath.dish.kid.test",
            "com.smithmicro.safepath.family.child"
    );

    public String detectSafePathPackage(List<String> installedPackages) {
        for (String installedPackage : installedPackages) {
            if (PACKAGE_HINTS.stream().anyMatch(installedPackage::contains)) {
                return installedPackage;
            }
        }
        return "";
    }

    public boolean isSupportedPackage(String installedPackage) {
        return SUPPORTED_PACKAGES.contains(installedPackage);
    }

    public List<String> supportedPackages() {
        return new ArrayList<>(SUPPORTED_PACKAGES);
    }
}
