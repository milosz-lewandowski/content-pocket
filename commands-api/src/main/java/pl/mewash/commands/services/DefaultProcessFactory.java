package pl.mewash.commands.services;

import pl.mewash.commands.api.CommandLogger;
import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.internals.CommandBuilder;
import pl.mewash.commands.internals.CommandBundles;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.Formats;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ContentProperties;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class DefaultProcessFactory implements ProcessFactory {

    private final String ytDlpCommandPath;
    private final String ffmpegCommandPath;
    private final CommandLogger logger;
    private final boolean printToConsole;

    public DefaultProcessFactory(String ytDlpCommandPath, String ffmpegCommandPath, CommandLogger logger,
                                 boolean printToConsole) {
        this.ytDlpCommandPath = ytDlpCommandPath;
        this.ffmpegCommandPath = ffmpegCommandPath;
        this.logger = logger;
        this.printToConsole = printToConsole;
    }

    public ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
                .addCommandBundle(CommandBundles.CHECK_CHANNEL_NAME)
                .setPrintToFile(channelProperties, tempFile)
                .setUrl(channelUrl)
                .logCommandToFile(logger)
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
                .logCommandToFile(logger)
                .printCommandToConsole(printToConsole)
                .buildReadOnlyProcess();
    }

    public ProcessBuilder downloadAudioStream(String url, AudioOnlyQuality audioQuality,
                                                          StorageOptions storageOptions, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
                .setAudioOnlySettings(audioQuality)
                .setOptionalFFMpegPath(ffmpegCommandPath, audioQuality.needsFFmpeg())
                .addOptionalCommandBundle(CommandBundles.EMBED_FILE_METADATA, audioQuality.getCanEmbedMetadata())
                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, storageOptions.withMetadataFiles())
                .setPrintToFile(ContentProperties.CONTENT_TITLE, tempFile)
                .setOutputCommand(storageOptions, audioQuality.getFormat())
                .setUrl(url)
                .logCommandToFile(logger)
                .printCommandToConsole(printToConsole)
                .buildDownloadProcess();
    }

    public ProcessBuilder downloadVideoWithAudioStream(String url, VideoQuality videoQuality,
                                                       StorageOptions storageOptions, Path tempFile) {
        return CommandBuilder.newYtDlpCommand(ytDlpCommandPath)
                .setVideoQuality(videoQuality)
                .addCommandBundle(CommandBundles.EXTRACT_BEST_AUDIO)
                .addCommandBundle(CommandBundles.EMBED_FILE_METADATA)
                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, storageOptions.withMetadataFiles())
                .setPrintToFile(ContentProperties.CONTENT_TITLE, tempFile)
                .setFFMpegPath(ffmpegCommandPath)
                .setOutputCommand(storageOptions, Formats.MP4)
                .setUrl(url)
                .logCommandToFile(logger)
                .printCommandToConsole(printToConsole)
                .buildDownloadProcess();
    }
}
