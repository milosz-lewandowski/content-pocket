package pl.mewash.common.app.config;

import pl.mewash.common.app.binaries.SupportedPlatforms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigPaths {

    private static final String MACOS_PROP_USER_HOME_PATTERN = "user.home";
    private static final String WIN_ENV_APPDATA_PATTERN = "APPDATA";

    private static final String POCKET_FILES_DIR_NAME = "PocketAppFiles";
    private static final String LOGS_DIR_NAME = "logs";

    private static final String SUBSCRIPTIONS_FILE_NAME = "subscriptions.json";
    private static final String SETTINGS_FILE_NAME = "settings.json";

    public static Path getSubscriptionsFilePath() throws IOException {
        SupportedPlatforms currentPlatform = SupportedPlatforms.getCurrentPlatform();
        return switch (currentPlatform) {
            case WINDOWS -> getWinLocalAppPocketDir().resolve(SUBSCRIPTIONS_FILE_NAME);
            case MACOS -> getMacLibAppSupportPocketDir().resolve(SUBSCRIPTIONS_FILE_NAME);
        };
    }

    public static Path getSettingsFilePath() throws IOException {
        SupportedPlatforms currentPlatform = SupportedPlatforms.getCurrentPlatform();
        return switch (currentPlatform) {
            case WINDOWS -> getWinAppDataPocketDir().resolve(SETTINGS_FILE_NAME);
            case MACOS -> getMacLibAppSupportPocketDir().resolve(SETTINGS_FILE_NAME);
        };
    }

    public static Path getLogsDirPath() throws IOException {
        SupportedPlatforms currentPlatform = SupportedPlatforms.getCurrentPlatform();
        Path logsDir = switch (currentPlatform) {
            case WINDOWS -> getWinLocalAppPocketDir().resolve(LOGS_DIR_NAME);
            case MACOS -> getMacLibAppSupportPocketDir().resolve(LOGS_DIR_NAME);
        };
        if (!Files.exists(logsDir)) Files.createDirectories(logsDir);
        return logsDir;
    }

    private static Path getWinLocalAppPocketDir() throws IOException {
        Path localPocketFilesDir = Paths
            .get("", POCKET_FILES_DIR_NAME)
            .toAbsolutePath();

        if (!Files.exists(localPocketFilesDir)) Files.createDirectories(localPocketFilesDir);
        return localPocketFilesDir;
    }

    private static Path getWinAppDataPocketDir() throws IOException {
        String appDataEnv = System.getenv(WIN_ENV_APPDATA_PATTERN);
        if (appDataEnv == null) throw new IllegalStateException("'%APPDATA%' could not be resolved!");

        Path appDataPocketDir = Paths
            .get(appDataEnv, POCKET_FILES_DIR_NAME)
            .toAbsolutePath();

        if (!Files.exists(appDataPocketDir)) {
            Files.createDirectories(appDataPocketDir);
        }
        return appDataPocketDir;
    }

    private static Path getMacLibAppSupportPocketDir() throws IOException {
        String userHomeProperty = System.getProperty(MACOS_PROP_USER_HOME_PATTERN);
        if (userHomeProperty == null) throw new IllegalStateException("'user.home' could not be resolved!");

        Path libAppSupportPocketDir = Paths
            .get(userHomeProperty, "Library", "Application Support", "ContentPocket", POCKET_FILES_DIR_NAME)
            .toAbsolutePath();

        if (!Files.exists(libAppSupportPocketDir)) Files.createDirectories(libAppSupportPocketDir);
        return libAppSupportPocketDir;
    }
}
