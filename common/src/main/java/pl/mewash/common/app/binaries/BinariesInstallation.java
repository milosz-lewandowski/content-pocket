package pl.mewash.common.app.binaries;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.file.Paths;

@NoArgsConstructor // for jackson
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BinariesInstallation {
    @Getter private SupportedPlatforms platform;
    @Getter private boolean confirmed;
    @Getter private Type installationType;
    @Getter private String binariesDirPath;

    public static BinariesInstallation confirmInDirectory(SupportedPlatforms platform, String binariesDirPath) {
        return new BinariesInstallation(platform, true, Type.DIRECTORY, binariesDirPath);
    }

    public static BinariesInstallation confirmInSysPath(SupportedPlatforms platform) {
        return new BinariesInstallation(platform, true, Type.SYS_PATH, null);
    }

    @JsonIgnore
    public String getBinaryCommand(BinariesNames binary) {
        if (!confirmed) throw new IllegalStateException("Binaries installation not confirmed!");

        return switch (installationType) {
            case DIRECTORY -> Paths.get(binariesDirPath, binary.getBinaryExecName(platform)).toAbsolutePath().toString();
            case SYS_PATH -> binary.getBinaryName(platform);
        };
    }

    @JsonIgnore
    public String getInstallationMessage() {
        if (!confirmed) return "Binaries installation is not confirmed!";
        return switch (installationType) {
            case SYS_PATH -> "Binaries installation confirmed in SYSTEM PATH";
            case DIRECTORY -> String.format("Binaries installation confirmed in DIRECTORY: \n'%s'", binariesDirPath);
        };
    }

    public enum Type {
        DIRECTORY,
        SYS_PATH,
    }
}
