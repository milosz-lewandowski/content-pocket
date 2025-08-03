package pl.mewash.contentlaundry.service;

import javafx.scene.control.Alert;
import pl.mewash.contentlaundry.commands.AudioOnlyQuality;
import pl.mewash.contentlaundry.commands.DownloadOption;
import pl.mewash.contentlaundry.commands.ProcessFactoryV2;
import pl.mewash.contentlaundry.commands.VideoQuality;
import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.ChannelSettings;
import pl.mewash.contentlaundry.models.content.FetchedContent;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;
import pl.mewash.contentlaundry.utils.AlertUtils;
import pl.mewash.contentlaundry.utils.ScheduledFileLogger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class DownloadService {

    private BiConsumer<String, Object[]> logConsumer;

    public void setLogConsumer(BiConsumer<String, Object[]> consumer) {
        this.logConsumer = consumer;
    }

    public void downloadFetched(FetchedContent content, DownloadOption downloadOption, String subsBasePath) {
        ChannelFetchRepo repository = ChannelFetchRepo.getInstance();

        ChannelSettings channelSettings = repository.getChannelSettings(content.getChannelName());
        GroupingMode byFormatGrouping = channelSettings.isSeparateDirPerFormat()
                ? GroupingMode.GROUP_BY_FORMAT
                : GroupingMode.NO_GROUPING;
        AdvancedOptions advOpt = new AdvancedOptions(
                false, byFormatGrouping, channelSettings.isAddDownloadDateDir(), MultithreadingMode.MEDIUM);

        try {
            Path channelBasePath = Paths.get(subsBasePath + File.separator + content.getChannelName());
            if (!Files.exists(channelBasePath)) Files.createDirectories(channelBasePath);

            Path savedPath = downloadWithSettings(content.getUrl(), downloadOption, channelBasePath.toString(), advOpt);

            content.addAndSetDownloaded(downloadOption, savedPath);
            repository.updateContent(content);
        } catch (Exception e) {
            e.printStackTrace();
            content.setDownloadingError(downloadOption);
            repository.updateContent(content);
            AlertUtils.showAlertAndAwait("Download error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public Path downloadWithSettings(String url, DownloadOption downloadSettings, String baseDirString,
                                     AdvancedOptions advancedOptions) throws IOException, InterruptedException {
        appendLog("log.washing_and_drying", url, downloadSettings.getShortDescription());

        // building paths
        Path baseDirPath = Paths.get(baseDirString).toAbsolutePath();
        long threadId = Thread.currentThread().threadId();
        Path tempDirPath = Files.createTempDirectory(baseDirPath, "__laundry-temp-multi-thread-" + threadId).toAbsolutePath();
        Path tempTitleFile = Files.createTempFile(tempDirPath, "temp_title", ".txt").toAbsolutePath();

        // Detect download type and get process
        ProcessBuilder processBuilder = switch (downloadSettings) {
            case VideoQuality vq -> ProcessFactoryV2.videoWithQualityDownload(url, vq, advancedOptions, tempTitleFile);
//            case VideoQuality vq -> ProcessFactoryV2.videoWithQualityDownloadForceH264(url, vq, advancedOptions, tempTitleFile);
            case AudioOnlyQuality aq -> ProcessFactoryV2.audioOnlyDownloadCommand(url, aq, advancedOptions, tempTitleFile);
            default -> throw new IllegalStateException("Settings type not recognized: " + downloadSettings);
        };

        // Run process
        processBuilder.directory(tempDirPath.toFile());
        Process process = processBuilder.start();

        // redirect and log process output stream to file while process is running
        ScheduledFileLogger.consumeAndLogProcessOutputToFile(process);

        // wait for process finished
        int exitCode = process.waitFor();

        // Get UTF-8 file title and delete temp file
        String title = Files.readString(tempTitleFile, StandardCharsets.UTF_8).trim();
        Files.deleteIfExists(tempTitleFile);

        // if process failed
        String fileExtension = downloadSettings.getFormatExtension();
        if (exitCode != 0) {
            System.err.println("Error downloading: " + url + " with title: " + title + " as " + fileExtension);
            appendLog("log.failed_to_process", url + " with title: " + title, fileExtension);
        }

        Path downloadedPath = moveTempContent(tempDirPath, baseDirPath);

        if (title.isEmpty()) {
            System.out.println("No download title: " + title + "." + fileExtension + " -> file probably already downloaded");
            appendLog("log.already_saved", title, fileExtension);
        } else {
            System.out.println("Downloaded: " + title + " as " + fileExtension);
            appendLog("log.laundry_ready", title, fileExtension);
        }

        cleanupTempDir(tempDirPath);
        return downloadedPath;
    }

//    public Path download(String url, Formats format, String baseDirString, AdvancedOptions advancedOptions) throws IOException, InterruptedException {
//        appendLog("log.washing_and_drying", url, format.fileExtension.toUpperCase());
//
//        // building paths
//        Path baseDirPath = Paths.get(baseDirString).toAbsolutePath();
//        long threadId = Thread.currentThread().threadId();
//        Path tempDirPath = Files.createTempDirectory(baseDirPath, "__laundry-temp-multi-thread-" + threadId).toAbsolutePath();
//        Path tempTitleFile = Files.createTempFile(tempDirPath, "temp_title", ".txt").toAbsolutePath();
//
//        // process creation + execution
//        ProcessBuilder builder = ProcessFactory.buildDownloadCommand(url, format, advancedOptions, tempTitleFile);
//        builder.directory(tempDirPath.toFile());
//        Process process = builder.start();
//
//        // redirect and log process output stream to file while process is running
//        LoggerUtils.synchronizedConsumeAndLogProcessOutputToFile(process);
//
//        // wait for process finished
//        int exitCode = process.waitFor();
//
//        // Get UTF-8 file title and delete temp file
//        String title = Files.readString(tempTitleFile, StandardCharsets.UTF_8).trim();
//        Files.deleteIfExists(tempTitleFile);
//
//        // if process failed
//        if (exitCode != 0) {
//            System.err.println("Error downloading: " + url + " with title: " + title + " as " + format.fileExtension);
//            appendLog("log.failed_to_process", url + " with title: " + title, format.fileExtension);
//        }
//
//        Path downloadPath = moveTempContent(tempDirPath, baseDirPath);
//
//        if (title.isEmpty()) {
//            System.out.println("No download title: " + title + "." + format.fileExtension + " -> file probably already downloaded");
//            appendLog("log.already_saved", title, format.fileExtension);
//        } else {
//            System.out.println("Downloaded: " + title + " as " + format.fileExtension);
//            appendLog("log.laundry_ready", title, format.fileExtension);
//        }
//
//        cleanupTempDir(tempDirPath);
//        return downloadPath;
//    }


    private void appendLog(String key, Object... params) {
        if (logConsumer != null) {
            logConsumer.accept(key, params);
        }
    }

    private Path moveTempContent(Path tempDir, Path basePath) throws IOException {
        AtomicReference<Path> firstMovedFile = new AtomicReference<>();
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

                            firstMovedFile.compareAndSet(null, target);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return firstMovedFile.get();
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