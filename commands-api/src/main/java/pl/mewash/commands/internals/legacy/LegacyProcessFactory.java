package pl.mewash.commands.internals.legacy;

import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.AudioOption;
import pl.mewash.commands.settings.formats.VideoOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class LegacyProcessFactory implements ProcessFactory {

    private final String ytDlpCommandPath;
    private final String ffmpegCommandPath;
    private final Consumer<String> commandLogger;
    private final boolean printToConsole;

    public LegacyProcessFactory(String ytDlpCommandPath, String ffmpegCommandPath, Consumer<String> commandLogger,
                                boolean printToConsole) {
        this.ytDlpCommandPath = ytDlpCommandPath;
        this.ffmpegCommandPath = ffmpegCommandPath;
        this.commandLogger = commandLogger;
        this.printToConsole = printToConsole;
    }

    public ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile) {
        return LegacyCmdBuilder.newYtDlpCommand(ytDlpCommandPath)
            .addCommandBundle(LegacyCmdBundles.CHECK_CHANNEL_NAME)
            .setPrintToFile(channelProperties, tempFile)
            .setUrl(channelUrl)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildReadOnlyProcess();
    }

    public ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate,
                                                      ContentProperties contentProperties, Path tempFile) {
        return LegacyCmdBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setDateAfter(afterDate)
            .addCommandBundle(LegacyCmdBundles.FETCH_CHANNEL_CONTENT)
            .setPrintToFile(contentProperties, tempFile)
            .setUrl(channelUrl)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildReadOnlyProcess();
    }

    public ProcessBuilder downloadAudioStream(String url, AudioOption audioOption,
                                              StorageOptions storageOptions, Path tempFile) {
        if (!(audioOption instanceof AudioOnlyQuality audioQuality))
            throw new IllegalArgumentException("Legacy Factory supports Only AudioOnlyQuality impl of AudioOption");

        return LegacyCmdBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setAudioOnlySettings(audioQuality)
            .setOptionalFFMpegPath(ffmpegCommandPath, audioQuality.getNeedsFFmpeg())
            .addOptionalCommandBundle(LegacyCmdBundles.EMBED_FILE_METADATA, audioQuality.getCanEmbedMetadata())
            .addOptionalCommandBundle(LegacyCmdBundles.ADD_CONTENT_DESCRIPTION_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_DESCRIPTION)
            .addOptionalCommandBundle(LegacyCmdBundles.ADD_CONTENT_METADATA_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_METADATA)
            .setPrintToFile(ContentProperties.CONTENT_TITLE, tempFile)
            .setOutputCommand(storageOptions, audioQuality)
            .setUrl(url)
            .logCommandWithLogger(commandLogger)
            .printCommandToConsole(printToConsole)
            .buildDownloadProcess();
    }

    public ProcessBuilder downloadVideoWithAudioStream(String url, VideoOption videoOption,
                                                       StorageOptions storageOptions, Path tempFile) {
        if (!(videoOption instanceof VideoQuality videoQuality))
            throw new IllegalArgumentException("Legacy Factory supports Only VideoQuality impl of VideoOption");

        return LegacyCmdBuilder.newYtDlpCommand(ytDlpCommandPath)
            .setVideoQuality(videoQuality)
            .addCommandBundle(LegacyCmdBundles.EXTRACT_BEST_AUDIO)
            .addCommandBundle(LegacyCmdBundles.EMBED_FILE_METADATA)
            .addOptionalCommandBundle(LegacyCmdBundles.ADD_CONTENT_DESCRIPTION_FILES,
                storageOptions.additionalFiles() == AdditionalFiles.MEDIA_WITH_DESCRIPTION)
            .addOptionalCommandBundle(LegacyCmdBundles.ADD_CONTENT_METADATA_FILES,
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
