package pl.mewash.common.app.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.mewash.common.app.binaries.BinariesInstallation;
import pl.mewash.common.app.config.ConfigPaths;
import pl.mewash.common.app.config.JsonMapperConfig;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.LoggersProvider;

import java.io.IOException;
import java.nio.file.Files;

public class SettingsManager {
    private static final FileLogger fileLogger = LoggersProvider.getFileLogger();
    private static final ObjectMapper mapper = JsonMapperConfig.getPrettyMapper();

    public static GeneralSettings load() {
        if (!Files.exists(ConfigPaths.SETTINGS_FILE)) return new GeneralSettings();

        try {
            ConfigPaths.ensureConfigDirExists();
            return mapper.readValue(ConfigPaths.SETTINGS_FILE.toFile(), GeneralSettings.class);
        } catch (IOException e) {
            fileLogger.logErrWithMessage("Failed to load settings: ", e, true);
            return new GeneralSettings();
        }
    }

    public static void saveSettings(GeneralSettings settings) {
        try {
            ConfigPaths.ensureConfigDirExists();
            mapper.writerWithDefaultPrettyPrinter()
                .writeValue(ConfigPaths.SETTINGS_FILE.toFile(), settings);
        } catch (IOException e) {
            fileLogger.logErrWithMessage("Failed to save settings: ", e, true);
        }
    }

    public static void saveBinariesInstallation(BinariesInstallation binariesInstallation) throws IOException {
        GeneralSettings generalSettings = load();
        generalSettings.setBinariesInstallation(binariesInstallation);

        ConfigPaths.ensureConfigDirExists();
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(ConfigPaths.SETTINGS_FILE.toFile(), generalSettings);
    }
}
