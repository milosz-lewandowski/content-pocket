package pl.mewash.common.downloads.impl;

import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.api.processes.ProcessFactoryProvider;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.api.DownloadService;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.temporary.CommandsDiffDetector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DefaultDownloadService implements DownloadService {

    private final ProcessFactory processFactory;
    private final FileLogger fileLogger;

    public DefaultDownloadService(AppContext appContext) {
        fileLogger = appContext.getFileLogger();
        processFactory = ProcessFactoryProvider.createDefaultFactoryWithLogger(
            appContext.getYtDlpCommand(), appContext.getFfMpegCommand(), fileLogger::appendSingleLine, true);
    }

    public DownloadResults downloadWithSettings(String url, DownloadOption downloadOpt, String baseDirString,
                                                StorageOptions storageOpt) throws IOException, InterruptedException {

        // Building paths
        Path baseDirPath = Paths.get(baseDirString).toAbsolutePath();
        long threadId = Thread.currentThread().threadId();
        Path tempDirPath = Files.createTempDirectory(baseDirPath, "__temp_laundry_" + threadId).toAbsolutePath();
        Path tempTitleFile = Files.createTempFile(tempDirPath, "__temp_laundry", ".txt").toAbsolutePath();

        // FIXME: TEMPORARY CHECKER
        CommandsDiffDetector commandsDiffDetector = new CommandsDiffDetector();
        switch (downloadOpt) {
            case VideoQuality vq -> commandsDiffDetector.downloadVideoWithAudioStream(url, vq, storageOpt, tempTitleFile);
            case AudioOnlyQuality aq -> commandsDiffDetector.downloadAudioStream(url, aq, storageOpt, tempTitleFile);
        };

        // Detect download type and get process
        ProcessBuilder processBuilder = switch (downloadOpt) {
            case VideoQuality vq -> processFactory.downloadVideoWithAudioStream(url, vq, storageOpt, tempTitleFile);
            case AudioOnlyQuality aq -> processFactory.downloadAudioStream(url, aq, storageOpt, tempTitleFile);
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


        if (exitCode != 0) fileLogger.appendSingleLine(String
            .format("[ERR] Exit code: %d of %s downloaded as %s", exitCode, url, downloadOpt.getOptionName()));

        Path downloadedPath = moveTempContent(tempDirPath, baseDirPath);
        cleanupTempDir(tempDirPath);

        return new DownloadResults(title, downloadedPath);
    }

    private Path moveTempContent(Path tempDir, Path basePath) throws IOException {
        Predicate<Path> metadataFileCheck = (path) -> path
            .getFileName().toString().endsWith(".description") || path.getFileName().toString().endsWith(".info.json");

        AtomicReference<Path> firstNonMetadataFile = new AtomicReference<>();

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
                            boolean isMetadataFile = metadataFileCheck.test(source);

                            if (isMetadataFile && Files.exists(target))
                                return; // skip if metadata file exists

                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                            if (!isMetadataFile)
                                firstNonMetadataFile.compareAndSet(null, target);
                        }
                    } catch (IOException ioe) {
                        fileLogger.logErrWithMessage("Error while moving files from temp dir", ioe, true);
                    }
                });
            return firstNonMetadataFile.get();
        }
    }

    private void cleanupTempDir(Path tempDir) {
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        fileLogger.logErrWithMessage("Cleanup error while deleting temp file", e, true);
                    }
                });
        } catch (IOException e) {
            fileLogger.logErrWithMessage("Cleanup error while traversing temp paths", e, true);
        }
    }
}