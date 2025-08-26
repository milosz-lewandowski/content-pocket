package pl.mewash.commands.internals;

import pl.mewash.commands.api.ProcessFactory;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Consumer;

public class NewProcessFactory implements ProcessFactory {

    private final String ytDlpCommandPath;
    private final String ffmpegCommandPath;
    private final Consumer<String> commandLogger;
    private final boolean printToConsole;

    public NewProcessFactory(String ytDlpCommandPath, String ffmpegCommandPath, Consumer<String> commandLogger,
                                 boolean printToConsole) {
        this.ytDlpCommandPath = ytDlpCommandPath;
        this.ffmpegCommandPath = ffmpegCommandPath;
        this.commandLogger = commandLogger;
        this.printToConsole = printToConsole;
    }

    @Override
    public ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile) {
        return CmdBuilder2.newYtDlpCommand(ytDlpCommandPath, printToConsole, commandLogger)
            .add(FetchCmd.Bundle.FETCH_BASIC_CHANNEL_DATA)
            .setFilePrint(PrintCmdEntry.withResponsePropsAndFile(channelProperties, tempFile))
            .buildFetch(channelUrl);
    }

    @Override
    public ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate,
                                                      ContentProperties contentProperties, Path tempFile) {
        return CmdBuilder2.newYtDlpCommand(ytDlpCommandPath, printToConsole, commandLogger)
            .add(FetchCmd.Bundle.fetchContentsDateAfter(afterDate))
            .setFilePrint(PrintCmdEntry.withResponsePropsAndFile(contentProperties, tempFile))
            .buildFetch(channelUrl);
    }

    @Override
    public ProcessBuilder downloadAudioStream(String url, AudioOnlyQuality audioQuality,
                                              StorageOptions storageOptions, Path tempFile) {
        return CmdBuilder2.newYtDlpCommand(ytDlpCommandPath, printToConsole, commandLogger)
            .setFilePrint(PrintCmdEntry.withResponsePropsAndFile(ContentProperties.CONTENT_TITLE, tempFile))
            .addOptional(CmdEntry.withParam(DownloadCmd.FFMPEG_LOCATION, ffmpegCommandPath), audioQuality.needsFFmpeg())
            .addOptional(DownloadCmd.Bundle.EMBED_PICTURE_AND_METADATA, audioQuality.getCanEmbedMetadata())
            .buildDownload(url, audioQuality, storageOptions);
    }

    @Override
    public ProcessBuilder downloadVideoWithAudioStream(String url, VideoQuality videoQuality,
                                                       StorageOptions storageOptions, Path tempFile) {
        return CmdBuilder2.newYtDlpCommand(ytDlpCommandPath, printToConsole, commandLogger)
            .setFilePrint(PrintCmdEntry.withResponsePropsAndFile(ContentProperties.CONTENT_TITLE, tempFile))
            .add(CmdEntry.withParam(DownloadCmd.FFMPEG_LOCATION, ffmpegCommandPath))
            .add(DownloadCmd.Bundle.EMBED_PICTURE_AND_METADATA)
            .buildDownload(url, videoQuality, storageOptions);
    }
}
