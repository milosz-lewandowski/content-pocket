package pl.mewash.contentlaundry.utils;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import pl.mewash.contentlaundry.models.general.GeneralSettings;
import pl.mewash.contentlaundry.subscriptions.SettingsManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class BinariesManager {

    public SupportedPlatforms getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) return SupportedPlatforms.MACOS;
        else if (os.contains("win")) return SupportedPlatforms.WINDOWS;
        else throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    public String resolveValidatedToolsDir(Stage mainStage) {
        GeneralSettings generalSettings = SettingsManager.load();
        String directoryPath = null;

        // if binaries confirmed
        if (generalSettings.binariesDirConfirmed) {
            directoryPath = generalSettings.binariesDirPath;
            return directoryPath;
        }

        // if not confirmed - check default locations
        directoryPath = switch (getPlatform()) {
            case MACOS -> findValidToolLocation(MacosLocations.values());
            case WINDOWS -> findValidToolLocation(WindowsLocations.values());
        };

        // if not found at default - ask for path in alert
        if (directoryPath == null) {
            String selectedBinariesPath = AlertUtils
                    .showBinariesNotFoundAlert(mainStage);
            boolean confirmedNew = verifyBinariesInDir(selectedBinariesPath);
            if (confirmedNew) directoryPath = selectedBinariesPath;
        }

        // if validated save and return
        if (directoryPath != null) {
            Path path = Paths.get(directoryPath);
            Path absolutePath = path.toAbsolutePath();
            directoryPath = absolutePath.toString();

            ScheduledFileLogger.appendSingleLine("Binaries found at: " + directoryPath);
            System.out.println("Binaries found at: " + directoryPath);
            generalSettings.binariesDirConfirmed = true;
            generalSettings.binariesDirPath = directoryPath;
            SettingsManager.saveSettings(generalSettings);
            return directoryPath;
        } else {
            AlertUtils.showAlertAndAwait("Binaries missing Error",
                    "To use this application you need to have installed yt-dlp, ffmpeg and ffprobe installed",
                    Alert.AlertType.ERROR);
            return null;
        }
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
        Map<BinariesNames, Path> existingPaths = Arrays.stream(BinariesNames.values())
                .map(requiredBinary -> Map.entry(requiredBinary, requiredBinary.getPathByLocation(location, platform)))
                .filter(entry -> Files.exists(entry.getValue())) // is it correct check?
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        if (existingPaths.size() != 3) {
            if (existingPaths.isEmpty()) logs.add("no binaries found at above location");
            if (!existingPaths.isEmpty() && existingPaths.size() < 3)
                logs.add("MISSING BINARIES! Found only " + existingPaths.size());
            ScheduledFileLogger.appendStringList(logs);
            return false;
        }

        Set<String> versionMessages = existingPaths.entrySet().stream()
                .map(entry -> getVersionResponse(entry.getKey(), entry.getValue()))
                .peek(message -> logs.add("version check result: " + message))
                .collect(Collectors.toSet());

        boolean result = versionMessages.size() == 3;
        if (!result) logs.add("Not all binaries returned version info!");
        ScheduledFileLogger.appendStringList(logs);
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
            e.printStackTrace();
        }

        return versionMessageBuilder.isEmpty()
                ? null
                : versionMessageBuilder.toString();
    }

    public enum SupportedPlatforms {
        WINDOWS,
        MACOS;
    }

    @FunctionalInterface
    interface ToolPathSupplier {
        String compilePath();
    }

    @AllArgsConstructor
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

    @AllArgsConstructor
    public enum MacosLocations implements ToolPathSupplier {
        USER_HOME_BIN("user.home", "bin"), // default for separate by user installation
//        HOME_DIR_BIN("home.dir", "bin"),
//        HOME_DIR_USR_BIN("home.dir", "usr/bin"),
//        APP_DIR_BIN("user.dir", "bin"),
        ;

        final String property;
        final String nextDir;

        public String compilePath() {
            String property = this.property;
            String nextDir = this.nextDir;
            return Paths.get(System.getProperty(property), nextDir).toString();
        }
    }

    @AllArgsConstructor
    public enum BinariesNames {
        YT_DLP("yt-dlp_macos", "yt-dlp.exe", "--version"),
        FFMPEG("ffmpeg", "ffmpeg.exe", "-version"),
        FFPROBE("ffprobe", "ffprobe.exe", "-version"),
        ;

        private final String macosName;
        private final String windowsName;
        private final String versionCommand;

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
