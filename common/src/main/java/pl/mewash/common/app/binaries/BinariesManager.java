package pl.mewash.common.app.binaries;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.LoggersProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BinariesManager {

    private final FileLogger fileLogger = LoggersProvider.getFileLogger();

    public SupportedPlatforms getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) return SupportedPlatforms.MACOS;
        else if (os.contains("win")) return SupportedPlatforms.WINDOWS;
        else throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    public String resolveToolsAtDefaultLocations(){
        GeneralSettings generalSettings = SettingsManager.load();
        String directoryPath = null;

        // if binaries confirmed
        if (generalSettings.isBinariesDirConfirmed()) {
            directoryPath = generalSettings.getBinariesDirPath();
            return directoryPath;
        }

        // if not confirmed - check default locations
        directoryPath = switch (getPlatform()) {
            case MACOS -> findValidToolLocation(MacosLocations.values());
            case WINDOWS -> findValidToolLocation(WindowsLocations.values());
        };

        if (directoryPath != null) {
            return saveValidToolsLocation(directoryPath);
        }
        return null;
    }

    public String resolveToolsAtGivenLocation(String toolsLocation){
        boolean confirmedNew = verifyBinariesInDir(toolsLocation);
        if (confirmedNew) {
            return saveValidToolsLocation(toolsLocation);
        } else throw new RuntimeException("Tools verification failed on given location: " + toolsLocation);
    }

    private String saveValidToolsLocation(String location){
        GeneralSettings generalSettings = SettingsManager.load();

        assert location != null;

        Path path = Paths.get(location);
        Path absolutePath = path.toAbsolutePath();
        location = absolutePath.toString();

        fileLogger.appendSingleLine("Binaries found at: " + location);
        System.out.println("Binaries found at: " + location);
        generalSettings.setBinariesDirConfirmed(true);
        generalSettings.setBinariesDirPath(location);
        SettingsManager.saveSettings(generalSettings);
        return location;
    }

    private <T extends Enum<T> & ToolPathSupplier> String findValidToolLocation(T[] locations) {
        return Arrays.stream(locations)
                .map(ToolPathSupplier::compilePath)
                .filter(this::verifyBinariesInDir)
                .findFirst()
                .orElse(null);
    }

    private boolean verifyBinariesInDir(String location) {
        List<String> logs = new ArrayList<>();
        logs.add("checking location: " + location);

        SupportedPlatforms platform = getPlatform();
        Map<BinariesNames, Path> existingPaths = BinariesNames.getObligatorySet().stream()
                .map(requiredBinary -> Map.entry(requiredBinary, requiredBinary.getPathByLocation(location, platform)))
                .filter(entry -> Files.exists(entry.getValue())) // is it correct check?
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int obligatoryCount = BinariesNames.getObligatoryCount();
        if (existingPaths.size() < obligatoryCount) {
            if (existingPaths.isEmpty()) logs.add("no binaries found at above location");
            if (!existingPaths.isEmpty())
                logs.add("MISSING BINARIES! Found only " + existingPaths.size());
            fileLogger.appendMultiLineStringList(logs);
            return false;
        }

        Set<String> versionMessages = existingPaths.entrySet().stream()
                .map(entry -> getVersionResponse(entry.getKey(), entry.getValue()))
                .peek(message -> logs.add("version check result: " + message))
                .collect(Collectors.toSet());

        boolean result = versionMessages.size() >= obligatoryCount;
        if (!result) logs.add("Not all binaries returned version info!");
        fileLogger.appendMultiLineStringList(logs);
        return result;
    }

    private String getVersionResponse(BinariesNames binary, Path binaryPath) {
        StringBuilder versionMessageBuilder = new StringBuilder();
        if (binary == BinariesNames.YT_DLP) System.out.println("check of yt-dlp version: ");
        try {
            String toolPathCommand = binaryPath.toAbsolutePath().toString();
            ProcessBuilder builder = new ProcessBuilder(toolPathCommand, binary.versionCommand);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                versionMessageBuilder.append("\n").append(line);
                System.out.println(line);
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("error while checking: " + binaryPath.toAbsolutePath());
            System.err.println(e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppContext.getInstance().getFileLogger()
                .appendSingleLine(sw.toString());
        }

        return versionMessageBuilder.isEmpty()
                ? null
                : versionMessageBuilder.toString();
    }

    public enum SupportedPlatforms {
        WINDOWS,
        MACOS;
    }

    sealed interface ToolPathSupplier permits WindowsLocations, MacosLocations {
        String compilePath();
    }

    @RequiredArgsConstructor
    public enum WindowsLocations implements ToolPathSupplier{
        APP_DIR_TOOLS("user.dir", "tools") // default for bundled zip
        ;

        final String property;
        final String nextDir;

        public String compilePath() {
            String property = this.property;
            String nextDir = this.nextDir;
            return Paths.get(System.getProperty(property), nextDir).toString();
        }
    }

    @RequiredArgsConstructor
    public enum MacosLocations implements ToolPathSupplier {
        USER_HOME_BIN("user.home", "bin"), // default for separate 'by user' installation
        ;

        final String property;
        final String nextDir;

        public String compilePath() {
            String property = this.property;
            String nextDir = this.nextDir;
            return Paths.get(System.getProperty(property), nextDir).toString();
        }
    }

    @RequiredArgsConstructor
    public enum BinariesNames {
        YT_DLP("yt-dlp_macos", "yt-dlp.exe", "--version", true),
        FFMPEG("ffmpeg", "ffmpeg.exe", "-version", true),
        FFPROBE("ffprobe", "ffprobe.exe", "-version", false),
        ;

        private final String macosName;
        private final String windowsName;
        private final String versionCommand;
        private final boolean isObligatory;

        @Getter private final static int obligatoryCount = (int) Arrays.stream(values())
            .filter(bn -> bn.isObligatory)
            .count();

        public static Set<BinariesNames> getObligatorySet() {
            return Arrays.stream(values())
                .filter(bn -> bn.isObligatory)
                .collect(Collectors.toSet());
        }

        public String getPlatformBinaryName(SupportedPlatforms platform) {
            return switch (platform) {
                case MACOS -> this.macosName;
                case WINDOWS -> this.windowsName;
            };
        }

        public Path getPathByLocation(String location, SupportedPlatforms platform) {
            return Paths.get(location, this.getPlatformBinaryName(platform));
        }
    }
}
