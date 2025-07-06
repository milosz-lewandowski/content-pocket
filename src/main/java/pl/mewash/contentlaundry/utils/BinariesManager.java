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

    public String validateAndGetBinariesDir(Stage mainStage) {
        GeneralSettings generalSettings = SettingsManager.load();
        String directoryPath = null;

        // if binaries confirmed
        if (generalSettings.alreadyConfirmed) {
            directoryPath = generalSettings.dirPath;
            return directoryPath;
        }

        // if not confirmed - check default locations
        directoryPath = switch (getPlatform()) {
            case MACOS -> {
                Optional<MacosLocations2> confirmedLocation = Arrays
                        .stream(MacosLocations2.values())
                        .filter(location -> confirmBinariesAtLocation(location.compilePath()))
                        .findFirst();
                yield confirmedLocation // is it always accessible?
                        .map(MacosLocations2::compilePath)
                        .orElse(null);
            }
            case WINDOWS -> {
                // TODO: implement
                Path TOOL_PATH = Paths.get(System.getProperty("user.dir"), "tools");
                yield TOOL_PATH.toString();
            }
        };

        // if not found at default - ask for path in alert
        if (directoryPath == null) {
            String selectedBinariesPath = AlertUtils
                    .showBinariesNotFoundAlert(mainStage);
            boolean confirmedNew = confirmBinariesAtLocation(selectedBinariesPath);
            if (confirmedNew) directoryPath = selectedBinariesPath;
        }

        // if validated save and return
        if (directoryPath != null) {
            LoggerUtils.synchronizedAppendStringList(List.of("Binaries found at: " + directoryPath));
            System.out.println("Binaries found at: " + directoryPath);
            generalSettings.alreadyConfirmed = true;
            generalSettings.dirPath = directoryPath;
            SettingsManager.saveSettings(generalSettings);
            return directoryPath;
        } else {
            AlertUtils.showAlertAndAwait("Binaries missing Error",
                    "To use this application you need to have installed yt-dlp, ffmpeg and ffprobe installed",
                    Alert.AlertType.ERROR);
            return null;
        }
    }

    private boolean confirmBinariesAtLocation(String location) {
        List<String> logs = new ArrayList<>();
        logs.add("checking location: " + location);

        SupportedPlatforms platform = getPlatform();
        Set<Path> existingPaths = Arrays.stream(BinariesNames.values())
                .map(requiredBinary -> requiredBinary.getPathByLocation(location, platform))
                .filter(Files::exists) // is it correct check?
//                .filter(Files::isExecutable)
                .collect(Collectors.toSet());

        if (existingPaths.size() != 3) {
            if (existingPaths.isEmpty()) logs.add("no binaries found at above location");
            if (!existingPaths.isEmpty() && existingPaths.size() < 3)
                logs.add("MISSING BINARIES! Found only " + existingPaths.size());
            LoggerUtils.synchronizedAppendStringList(logs);
            return false;
        }

        Set<String> versionMessages = existingPaths.stream()
                .map(BinariesManager::validateBinaryVersion)
                .peek(message -> logs.add("version check result: " + message))
                .collect(Collectors.toSet());

        boolean result = versionMessages.size() == 3;
        if (!result) logs.add("Not all binaries returned version info!");
        LoggerUtils.synchronizedAppendStringList(logs);
        return result;
    }



    public static String validateBinaryVersion(Path binaryPath) {
        StringBuilder versionMessageBuilder = new StringBuilder();
        try {
            String toolPathCommand = binaryPath.toAbsolutePath().toString();
            ProcessBuilder builder = new ProcessBuilder(toolPathCommand, "--version");
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

    private SupportedPlatforms getPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) return SupportedPlatforms.MACOS;
        else if (os.contains("win")) return SupportedPlatforms.WINDOWS;
        else throw new UnsupportedOperationException("Unsupported OS: " + os);
    }

    enum SupportedPlatforms {
        WINDOWS,
        MACOS;
    }

//    @AllArgsConstructor
//    enum MacosLocations {
//        HOME_BIN_PROP(Paths.get(System.getProperty("home.dir"), "bin").toString(), false),
//        HOME_USER_BIN_PROP(Paths.get(System.getProperty("home.dir"), "usr", "bin").toString(), false),
//        USR_LOCAL("/usr/local/bin/", false),
//        USER_HOME_BIN_PROP(Paths.get(System.getProperty("user.dir"), "bin").toString(), false),
//        USER_HOME_BIN_PROP_2(Paths.get(System.getProperty("user.dir"), "bin").toString(), false),
//
////        OPT_HOMEBREW("/opt/homebrew/bin/", false),
////        USR_BIN("/usr/bin/", false),              // system only probably
//
//        APP_DIR_PROPERTY(Paths.get(System.getProperty("user.dir"), "tools", "mac").toString(), true),
//        RESOURCES("../Resources/tools/mac/", true)
////        ,USER_DIR(System.getProperty("user.dir"), "tools", "mac");
////        ,JPACKAGE("src/main/jpackage/tools/mac", true);
//        ;
//
//        final String dirPath;
//        final boolean bundled;
//
//        private String compilePath(){
//            String property = this.property;
//            String nextDir = this.nextDir;
//        }
//    }

    @AllArgsConstructor
    enum MacosLocations2 {
        HOME_BIN_PROP("user.home", "bin"),
//        HOME_BIN_PROP("home.dir", "bin"),
//        HOME_USER_BIN_PROP("home.dir", "usr/bin"),
////        USR_LOCAL("/usr/local/bin/", false),
//        USER_HOME_BIN_PROP("user.dir", "bin"),
//        USER_HOME_BIN_PROP_2("user.dir", "bin"),
        ;

        final String property;
        final  String nextDir;

        private String compilePath(){
            String property = this.property;
            String nextDir = this.nextDir;
            return Paths.get(System.getProperty(property),  nextDir).toString();
        }
    }

    @AllArgsConstructor
    enum BinariesNames {
        YT_DLP("yt-dlp_macos", "yt-dlp.exe"),
        FFMPEG("ffmpeg", "ffmpeg.exe"),
        FFPROBE("ffprobe", "ffprobe.exe"),
        ;

        private final String macosName;
        private final String windowsName;

        public String getPlatformName(SupportedPlatforms platform) {
            return switch (platform) {
                case MACOS -> this.macosName;
                case WINDOWS -> this.windowsName;
            };
        }

        public Path getPathByLocation(String location, SupportedPlatforms platform) {
            return Paths.get(location, this.getPlatformName(platform));
        }
    }
}
