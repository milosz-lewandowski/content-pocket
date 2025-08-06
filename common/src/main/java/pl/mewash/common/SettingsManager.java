package pl.mewash.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;

public class SettingsManager {
    private static final ObjectMapper mapper = JsonMapperConfig.getPrettyMapper();

    public static GeneralSettings load() {
        if (!Files.exists(ConfigPaths.SETTINGS_FILE)) return new GeneralSettings();

        try {
            ConfigPaths.ensureConfigDirExists();
            return mapper.readValue(ConfigPaths.SETTINGS_FILE.toFile(), GeneralSettings.class);
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
            return new GeneralSettings();
        }
    }

    public static void saveSettings(GeneralSettings settings) {
        try {
            ConfigPaths.ensureConfigDirExists();
            mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValue(ConfigPaths.SETTINGS_FILE.toFile(), settings);
        } catch (IOException e){
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }
}
