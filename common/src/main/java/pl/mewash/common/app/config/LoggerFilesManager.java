package pl.mewash.common.app.config;

import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.common.logging.api.LoggersProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LoggerFilesManager {

    private static Path cachedLocalLogsDir;

    public static Path getResolvedLogFile() throws IOException {
        Path logDir = getResolvedLocalLogsDir();
        String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        return logDir.resolve(date + ".log");
    }

    public static Path getResolvedLocalLogsDir() throws IOException {
        if (cachedLocalLogsDir != null) return cachedLocalLogsDir;

        GeneralSettings settings = SettingsManager.loadSettings();
        String savedLocalLogsDirString = settings.getSavedLocalLogsDirPath();

        if (savedLocalLogsDirString != null) {
            Path savedLocalLogsDir = Paths.get(savedLocalLogsDirString);
            if (Files.exists(savedLocalLogsDir)) {
                cachedLocalLogsDir = savedLocalLogsDir;
                return cachedLocalLogsDir;
            }
        }
        cachedLocalLogsDir = ConfigPaths.getLogsDirPath();
        LoggersProvider.getFileLogger()
            .appendSingleLine("Loading logs directory from:\n" + cachedLocalLogsDir);

        settings.setSavedLocalLogsDirPath(cachedLocalLogsDir.toAbsolutePath().toString());
        SettingsManager.saveSettings(settings);

        return cachedLocalLogsDir;
    }
}
