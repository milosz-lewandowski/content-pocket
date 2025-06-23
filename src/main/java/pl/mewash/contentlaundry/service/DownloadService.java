package pl.mewash.contentlaundry.service;

import pl.mewash.contentlaundry.models.content.FetchedUpload;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.utils.LoggerUtils;
import pl.mewash.contentlaundry.utils.ProcessFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.BiConsumer;

public class DownloadService {

    private BiConsumer<String, Object[]> logConsumer;

    public void setLogConsumer(BiConsumer<String, Object[]> consumer) {
        this.logConsumer = consumer;
    }

    public void downloadFetched(FetchedUpload fetchedUpload, Formats format, String basePathString,
                                AdvancedOptions advancedOptions) throws IOException, InterruptedException {
        String url = fetchedUpload.getUrl();

        //TODO: here do all required contents history and tracking management

        // run download
        download(url, format, basePathString, advancedOptions);

    }

    public void download(String url, Formats format, String baseDirString, AdvancedOptions advancedOptions) throws IOException, InterruptedException {
        appendLog("log.washing_and_drying", url, format.name());

        // building paths
        Path baseDirPath = Paths.get(baseDirString);
        long threadId = Thread.currentThread().threadId();
        Path tempDirPath = Files.createTempDirectory(baseDirPath, "__laundry-temp-multi-thread-" + threadId);
        Path tempTitleFile = Files.createTempFile(tempDirPath, "temp_title", ".txt");

        // process creation + execution
        ProcessBuilder builder = ProcessFactory.buildDownloadCommand(url, format, advancedOptions, tempTitleFile);
        builder.directory(tempDirPath.toFile());
        Process process = builder.start();

        // redirect and log process output stream to file while process is running
        LoggerUtils.synchronizedLogProcessOutputToFile(process);

        // wait for process finished
        int exitCode = process.waitFor();

        // Get UTF-8 file title and delete temp file
        String title = Files.readString(tempTitleFile, StandardCharsets.UTF_8).trim();
        Files.deleteIfExists(tempTitleFile);

        // if process failed
        if (exitCode != 0) {
            System.err.println("Error downloading: " + url + " with title: " + title + " as " + format.value);
            appendLog("log.failed_to_process", url + " with title: " + title, format.value);
        }

        moveTempContent(tempDirPath, baseDirPath);

        if (title.isEmpty()) {
            System.out.println("No download title: " + title + "." + format.value + " -> file probably already downloaded");
            appendLog("log.already_saved", title, format.value);
        } else {
            System.out.println("Downloaded: " + title + " as " + format.value);
            appendLog("log.laundry_ready", title, format.value);
        }

        cleanupTempDir(tempDirPath);
    }


    private void appendLog(String key, Object... params) {
        if (logConsumer != null) {
            logConsumer.accept(key, params);
        }
    }

    private void moveTempContent(Path tempDir, Path basePath) throws IOException {
        Files.walk(tempDir)
                .filter(path -> !path.equals(tempDir))
                .forEach(source -> {
                    try {
                        Path relativePath = tempDir.relativize(source);
                        Path target = basePath.resolve(relativePath);
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))  // delete children first
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}