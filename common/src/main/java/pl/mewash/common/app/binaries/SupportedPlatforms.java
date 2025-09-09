package pl.mewash.common.app.binaries;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.List;

@RequiredArgsConstructor
public enum SupportedPlatforms {
    WINDOWS(List.of(
        new DefaultToolPath("user.dir", "tools")    // default for bundled zip
    )),
    MACOS(List.of(
        new DefaultToolPath("user.home", "bin")     // default for separate 'by user' installation
    ));

    @Getter private final List<DefaultToolPath> defaultToolPaths;

    private static SupportedPlatforms detectedPlatform;

    public static SupportedPlatforms getCurrentPlatform() {
        if (detectedPlatform != null) return detectedPlatform;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) detectedPlatform = SupportedPlatforms.MACOS;
        else if (os.contains("win")) detectedPlatform = SupportedPlatforms.WINDOWS;
        else throw new UnsupportedOperationException("Unsupported OS: " + os);

        return detectedPlatform;
    }

    public record DefaultToolPath(String property, String nextDirs) {
        public String compilePath() {
            return Paths.get(System.getProperty(property), nextDirs).toString();
        }
    }
}
