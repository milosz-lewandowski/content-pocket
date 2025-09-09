package pl.mewash.common.app.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.mewash.common.app.binaries.BinariesInstallation;
import pl.mewash.common.app.binaries.SupportedPlatforms;
import pl.mewash.common.app.config.ConfigPaths;
import pl.mewash.common.app.config.JsonMapperConfig;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.LoggersProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsManager {

    private static final FileLogger fileLogger = LoggersProvider.getFileLogger();
    private static final ObjectMapper mapper = JsonMapperConfig.getPrettyMapper();

    private static Path cachedSettingsPath;

    public static GeneralSettings loadSettings() {
        try {
            Path settingsFilePath = resolveSettingsFilePath();
            if (!Files.exists(settingsFilePath)) return new GeneralSettings();
            else return mapper.readValue(settingsFilePath.toFile(), GeneralSettings.class);

        } catch (IOException e) {
            fileLogger.logErrWithMessage("Failed to load settings: ", e, true);
            return new GeneralSettings();
        }
    }

    public static void saveSettings(GeneralSettings settings) {
        try {
            Path settingsFilePath = resolveSettingsFilePath();
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(settingsFilePath.toFile(), settings);

        } catch (IOException e) {
            fileLogger.logErrWithMessage("Failed to save settings: ", e, true);
        }
    }

    public static void saveBinariesInstallation(BinariesInstallation binariesInstallation) {
        if (binariesInstallation.getPlatform() != SupportedPlatforms.getCurrentPlatform())
            throw new RuntimeException("Detected platforms mismatch!");

        GeneralSettings loadedSettings = loadSettings();
        loadedSettings.setBinariesInstallation(binariesInstallation);

        saveSettings(loadedSettings);
    }

    private static Path resolveSettingsFilePath() throws IOException{
            if (cachedSettingsPath != null) return cachedSettingsPath;

            cachedSettingsPath = ConfigPaths.getSettingsFilePath();
            LoggersProvider.getFileLogger()
                .appendSingleLine("Loading app settings from:\n" + cachedSettingsPath);

            return cachedSettingsPath;
    }
}
