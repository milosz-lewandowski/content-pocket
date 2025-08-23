package pl.mewash.commands.services;

import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.internals.CommandBuilder;
import pl.mewash.commands.internals.CommandBundles;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class DefaultProcessFactory implements ProcessFactory {

    private final String ytDlpCommandPath;
    private final String ffmpegCommandPath;
    private final Consumer<String> commandLogger;
    private final boolean printToConsole;

    public DefaultProcessFactory(String ytDlpCommandPath, String ffmpegCommandPath, Consumer<String> commandLogger,
                                 boolean printToConsole) {
        this.ytDlpCommandPath = ytDlpCommandPath;
        this.ffmpegCommandPath = ffmpegCommandPath;
        this.commandLogger = commandLogger;
        this.printToConsole = printToConsole;
    }

    public ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
            .addCommandBundle(CommandBundles.CHECK_CHANNEL_NAME)
            .setPrintToFile(channelProperties, tempFile)
            .setUrl(channelUrl)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildReadOnlyProcess();
    }

    public ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate,
                                                      ContentProperties contentProperties, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setDateAfter(afterDate)
            .addCommandBundle(CommandBundles.FETCH_CHANNEL_CONTENT)
            .setPrintToFile(contentProperties, tempFile)
            .setUrl(channelUrl)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildReadOnlyProcess();
    }

    public ProcessBuilder downloadAudioStream(String url, AudioOnlyQuality audioQuality,
                                              StorageOptions storageOptions, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setAudioOnlySettings(audioQuality)
            .setOptionalFFMpegPath(ffmpegCommandPath, audioQuality.needsFFmpeg())
            .addOptionalCommandBundle(CommandBundles.EMBED_FILE_METADATA, audioQuality.getCanEmbedMetadata())
            .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_DESCRIPTION_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_DESCRIPTION)
            .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_METADATA)
            .setPrintToFile(ContentProperties.CONTENT_TITLE, tempFile)
            .setOutputCommand(storageOptions, audioQuality)
            .setUrl(url)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildDownloadProcess();
    }

    public ProcessBuilder downloadVideoWithAudioStream(String url, VideoQuality videoQuality,
                                                       StorageOptions storageOptions, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setVideoQuality(videoQuality)
            .addCommandBundle(CommandBundles.EXTRACT_BEST_AUDIO)
            .addCommandBundle(CommandBundles.EMBED_FILE_METADATA)
            .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_DESCRIPTION_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_DESCRIPTION)
            .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_METADATA)
            .setPrintToFile(ContentProperties.CONTENT_TITLE, tempFile)
            .setFFMpegPath(ffmpegCommandPath)
            .setOutputCommand(storageOptions, videoQuality)
            .setUrl(url)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildDownloadProcess();
    }
}
