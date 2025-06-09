package pl.mewash.contentlaundry.service;

import pl.mewash.contentlaundry.models.AdvancedOptions;
import pl.mewash.contentlaundry.utils.Formats;
import pl.mewash.contentlaundry.utils.ProcessFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public void download(String url, Formats format, String basePathString, AdvancedOptions advancedOptions) throws IOException, InterruptedException {
        Path basePath = Paths.get(basePathString);
        long threadId = Thread.currentThread().threadId();
        Path tempDirPath = Files.createTempDirectory(basePath, "__laundry-temp-multi-thread-" + threadId);
        System.out.println(tempDirPath.toString()); // test

        appendLog("log.washing_and_drying", url, format.name());

        ProcessBuilder builder = ProcessFactory.buildProcessCommand(url, format, advancedOptions);
        builder.directory(tempDirPath.toFile());

        Process process = builder.start();
        String title = getTitle(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Error downloading: " + url + " with title: " + title + " as " + format.value);
            appendLog("log.failed_to_process", url + " with title: " + title, format.value);
        }

        moveTempContent(tempDirPath, basePath);

        if (title == null || title.isEmpty()) {
            System.out.println("No download title: " + title + "." + format.value + " -> file probably already downloaded");
            appendLog("log.already_saved", title, format.value);
        } else {
            System.out.println("Downloaded: " + title + " as " + format.value);
            appendLog("log.laundry_ready", title, format.value);
        }

        cleanupTempDir(tempDirPath);
    }

    private static String getTitle(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        String title = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("[download] Destination: ")) {
                String filename = line.substring("[download] Destination: ".length()).trim();
                int dotIndex = filename.lastIndexOf('.');
                title = (dotIndex > 0) ? filename.substring(0, dotIndex) : filename;
            }
        }
        return title;
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
                        } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }
}