package pl.mewash.common.app.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigPaths {
    public static final Path BASE_DIR = Paths.get("", "LaundryAppFiles").toAbsolutePath();
    public static final Path LOGS_DIR = BASE_DIR.resolve("logs");
    public static final Path CONFIG_DIR = BASE_DIR.resolve("config");

    public static final Path SETTINGS_FILE = CONFIG_DIR.resolve("settings.json");
    public static final Path SUBSCRIPTIONS_FILE = CONFIG_DIR.resolve("subscriptions.json");

    public static void ensureConfigDirExists() throws IOException {
        if (!Files.exists(CONFIG_DIR)) {
            Files.createDirectories(CONFIG_DIR);
        }
    }

    public static Path getLogsDir() throws IOException {
        return Files.exists(LOGS_DIR)
                ? LOGS_DIR
                : Files.createDirectories(LOGS_DIR);
    }
}
