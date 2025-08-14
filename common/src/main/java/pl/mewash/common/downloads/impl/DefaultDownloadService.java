package pl.mewash.common.downloads.impl;

import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.api.ProcessFactoryProvider;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.api.DownloadService;
import pl.mewash.common.logging.api.FileLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultDownloadService implements DownloadService {

    private final ProcessFactory processFactory;
    private final FileLogger fileLogger;

    private BiConsumer<String, Object[]> logConsumer;

    public DefaultDownloadService(AppContext appContext) {
        fileLogger = appContext.getFileLogger();
        Consumer<String> commandLogger = fileLogger::appendSingleLine;
        processFactory = ProcessFactoryProvider.createDefaultWithConsolePrintAndLogger(
            appContext.getYtDlpCommand(), appContext.getFfMpegCommand(), commandLogger, false);
    }

    public void injectLogger(BiConsumer<String, Object[]> consumer) {
        this.logConsumer = consumer;
    }

    public Path downloadWithSettings(String url, DownloadOption downloadSettings, String baseDirString,
                                     StorageOptions storageOptions) throws IOException, InterruptedException {
        appendLog("log.washing_and_drying", url, downloadSettings.getOptionName());

        // building paths
        Path baseDirPath = Paths.get(baseDirString).toAbsolutePath();
        long threadId = Thread.currentThread().threadId();
        Path tempDirPath = Files.createTempDirectory(baseDirPath, "__laundry-temp-multi-thread-" + threadId).toAbsolutePath();
        Path tempTitleFile = Files.createTempFile(tempDirPath, "temp_title", ".txt").toAbsolutePath();

        // Detect download type and get process
        ProcessBuilder processBuilder = switch (downloadSettings) {
            case VideoQuality vq -> processFactory.downloadVideoWithAudioStream(url, vq, storageOptions, tempTitleFile);
            case AudioOnlyQuality aq -> processFactory.downloadAudioStream(url, aq, storageOptions, tempTitleFile);
        };

        // Run process
        processBuilder.directory(tempDirPath.toFile());
        Process process = processBuilder.start();

        // Redirect and log process output stream to file while process is running
        fileLogger.consumeAndLogProcessOutputToFile(process);

        // Wait for process finished
        int exitCode = process.waitFor();

        // Get UTF-8 file title and delete temp file
        String title = Files.readString(tempTitleFile, StandardCharsets.UTF_8).trim();
        Files.deleteIfExists(tempTitleFile);

        // if process failed
        String fileExtension = downloadSettings.getOptionName();
        if (exitCode != 0) {
            System.err.println("Error downloading: " + url + " with title: " + title + " as " + fileExtension);
            appendLog("log.failed_to_process", url + " with title: " + title, fileExtension);
        }

        Path downloadedPath = moveTempContent(tempDirPath, baseDirPath);

        if (title.isEmpty()) {
            System.out.println("No download title: " + title + " " + fileExtension + " -> file probably already downloaded");
            appendLog("log.already_saved", title, fileExtension);
        } else {
            System.out.println("Downloaded: " + title + " as " + fileExtension);
            appendLog("log.laundry_ready", title, fileExtension);
        }

        cleanupTempDir(tempDirPath);
        return downloadedPath;
    }

    private void appendLog(String key, Object... params) {
        if (logConsumer != null) {
            logConsumer.accept(key, params);
        }
    }

    private Path moveTempContent(Path tempDir, Path basePath) throws IOException {
        Predicate<Path> isMetadataFile = (path) -> path
            .getFileName().toString().endsWith(".description") || path.getFileName().toString().endsWith(".info.json");

        AtomicReference<Path> firstMovedFile = new AtomicReference<>();

        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.filter(path -> !path.equals(tempDir))
                .forEach(source -> {
                    try {
                        Path relativePath = tempDir.relativize(source);
                        Path target = basePath.resolve(relativePath);

                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                            if (isMetadataFile.test(source) && Files.exists(target))
                                return; // skip if metadata file exists

                            firstMovedFile.compareAndSet(null, target);
                        }
                    } catch (IOException ioe) {
                        fileLogger.appendSingleLine(ioe.getMessage());
                    }
                });
            return firstMovedFile.get();
        }
    }

    private void cleanupTempDir(Path tempDir) {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted((a, b) -> b.compareTo(a))  // delete children first
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        fileLogger.appendSingleLine(e.getMessage());
                    }
                });
        } catch (IOException e) {
            fileLogger.appendSingleLine(e.getMessage());
        }
    }
}