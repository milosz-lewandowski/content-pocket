package pl.mewash.common.app.binaries;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum BinariesNames {
    YT_DLP("yt-dlp_macos", "yt-dlp", "--version", true),
    FFMPEG("ffmpeg", "ffmpeg", "-version", true),
    FFPROBE("ffprobe", "ffprobe", "-version", true),
    ;

    private final String macosName;
    private final String windowsName;
    @Getter private final String versionCommand;
    private final boolean isObligatory;

    @Getter private final static int obligatoryCount = (int) Arrays.stream(values())
        .filter(bn -> bn.isObligatory)
        .count();

    public static Set<BinariesNames> getObligatorySet() {
        return Arrays.stream(values())
            .filter(bn -> bn.isObligatory)
            .collect(Collectors.toSet());
    }

    public String getBinaryName(SupportedPlatforms platform) {
        return switch (platform) {
            case MACOS -> this.macosName;
            case WINDOWS -> this.windowsName;
        };
    }

    public String getBinaryExecName(SupportedPlatforms platform) {
        return switch (platform) {
            case MACOS -> this.macosName;
            case WINDOWS -> this.windowsName + ".exe";
        };
    }

    Path resolveExecutablePath(String location, SupportedPlatforms platform) {
        return Paths.get(location, this.getBinaryExecName(platform));
    }
}
