package pl.mewash.contentlaundry.commands;

import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.Formats;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class ProcessFactoryV2 {

//    public static ProcessBuilder buildCheckChannelCommand(String channelUrl, Path tempFile) {
//        return CommandBuilder.newYtDlpCommand()
//                .addCommandBundle(CommandBundles.CHECK_CHANNEL_NAME)
//                .setPrintToFile(CommandBuilder.PrintToFileOptions.CHANNEL_NAME, tempFile)
//                .setUrl(channelUrl)
//                .logCommandToConsole()
//                .buildReadOnlyProcess();
//    }

    public static ProcessBuilder buildCheckChannelAndLatestContent(String channelUrl, Path tempFile) {
        return CommandBuilder.newYtDlpCommand()
                .addCommandBundle(CommandBundles.CHECK_CHANNEL_NAME)
                .setPrintToFile(CommandBuilder.PrintToFileOptions.CHANNEL_NAME_LATEST_CONTENT, tempFile)
                .setUrl(channelUrl)
                .logCommandToFile()
                .logCommandToConsole()
                .buildReadOnlyProcess();
    }

    public static ProcessBuilder buildFetchUploadListCommand(String channelUrl, LocalDateTime afterDate, Path tempFile) {
        return CommandBuilder.newYtDlpCommand()
                .setDateAfter(afterDate)
                .addCommandBundle(CommandBundles.FETCH_CHANNEL_CONTENT)
                .setPrintToFile(CommandBuilder.PrintToFileOptions.CONTENT_PROPERTIES, tempFile)
                .setUrl(channelUrl)
                .logCommandToConsole()
                .buildReadOnlyProcess();
    }

    public static ProcessBuilder audioOnlyDownloadCommand(String url, AudioOnlyQuality audioQuality,
                                                          AdvancedOptions advancedOptions, Path tempFile) {

        return CommandBuilder.newYtDlpCommand()
                .setAudioOnlySettings(audioQuality)
                .setOptionalFFMpegPath(audioQuality.needsFFmpeg())
                .addOptionalCommandBundle(CommandBundles.EMBED_FILE_METADATA, audioQuality.canEmbedMetadata)
                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, advancedOptions.withMetadata())
                .setPrintToFile(CommandBuilder.PrintToFileOptions.CONTENT_TITLE, tempFile)
                .setOutputCommand(advancedOptions, audioQuality.getFormat())
                .setUrl(url)
                .logCommandToFile()
                .logCommandToConsole()
                .buildDownloadProcess();
    }

    public static ProcessBuilder videoWithQualityDownload(String url, VideoQuality videoQuality,
                                                          AdvancedOptions advancedOptions, Path tempFile) {
        return CommandBuilder.newYtDlpCommand()
                .setVideoQuality(videoQuality)
                .addCommandBundle(CommandBundles.EXTRACT_BEST_AUDIO)
                .addCommandBundle(CommandBundles.EMBED_FILE_METADATA)
                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, advancedOptions.withMetadata())
                .setPrintToFile(CommandBuilder.PrintToFileOptions.CONTENT_TITLE, tempFile)
                .setFFMpegPath()
                .setOutputCommand(advancedOptions, Formats.MP4)
                .setUrl(url)
                .logCommandToFile()
                .logCommandToConsole()
                .buildDownloadProcess();
    }

//    public static ProcessBuilder videoWithQualityDownloadForceH264(String url, VideoQuality videoQuality,
//                                                          AdvancedOptions advancedOptions, Path tempFile) {
//        return CommandBuilder.newYtDlpCommand()
//                .setVideoQuality(videoQuality)
//                .addCommandBundle(CommandBundles.FORCE_H264_RECODING)
//                .addCommandBundle(CommandBundles.EMBED_FILE_METADATA)
//                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, advancedOptions.withMetadata())
//                .setPrintToFile(CommandBuilder.PrintToFileOptions.CONTENT_TITLE, tempFile)
//                .setFFMpegPath()
//                .setOutputCommand(advancedOptions, Formats.MP4)
//                .setUrl(url)
//                .logCommandToConsole()
//                .buildDownloadProcess();
//    }

//    @Deprecated
//    public static ProcessBuilder buildDownloadCommand(String url, Formats format,
//                                                      AdvancedOptions advancedOptions, Path tempTitleFile) {
//        return CommandBuilder.newYtDlpCommand()
//                .addCommandBundle(CommandBundles.ONLY_AUDIO_BEST_QUALITY)
//                .addParametrizedCommand("--audio-format", format.fileExtension)
//                .addCommandBundle(CommandBundles.EMBED_FILE_METADATA)
//                .addOptionalCommandBundle(CommandBundles.ADD_CONTENT_METADATA_FILES, advancedOptions.withMetadata())
//                .setFFMpegPath()
//                .setPrintToFile(CommandBuilder.PrintToFileOptions.CONTENT_TITLE, tempTitleFile)
//                .setOutputCommand(advancedOptions, format)
//                .setUrl(url)
//                .buildDownloadProcess();
//    }
}
