package pl.mewash.commands.internals.legacy;

import pl.mewash.commands.api.entries.OutputPatternResolver;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ResponseProperties;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LegacyCmdBuilder {

    private final List<String> commandList;
    private List<String> outputCommand;
    private List<String> printCommand;
    private String url;
    private boolean printToConsole = false;

    private boolean logWithLogger = false;
    private Consumer<String> commandLogger;

    private LegacyCmdBuilder() {
        this.commandList = new ArrayList<>();
    }

    public static LegacyCmdBuilder newYtDlpCommand(String ytDlpCommandPath) {
        return new LegacyCmdBuilder().addSingleCommand(ytDlpCommandPath);
    }

    public LegacyCmdBuilder addSingleCommand(String command) {
        this.commandList.add(command);
        return this;
    }

    public LegacyCmdBuilder printCommandToConsole(boolean printToConsole) {
        this.printToConsole = printToConsole;
        return this;
    }

    public LegacyCmdBuilder logCommandWithLogger(Consumer<String> logger) {
        this.commandLogger = logger;
        this.logWithLogger = true;
        return this;
    }

    public LegacyCmdBuilder addParametrizedCommand(String command, String parameter) {
        this.commandList.add(command);
        this.commandList.add(parameter);
        return this;
    }

    public LegacyCmdBuilder addCommandBundle(LegacyCmdBundles bundle) {
        bundle.appendAtEndOf(this.commandList);
        return this;
    }

    public LegacyCmdBuilder addOptionalCommandBundle(LegacyCmdBundles bundle, boolean isSelected) {
        if (isSelected) bundle.appendAtEndOf(this.commandList);
        return this;
    }

    public LegacyCmdBuilder addCommandList(List<String> listOfCommands) {
        this.commandList.addAll(listOfCommands);
        return this;
    }

    public LegacyCmdBuilder setVideoQuality(VideoQuality videoQuality) {
        this.addCommandList(videoQuality.getDownloadCommand());
        return this;
    }

    public LegacyCmdBuilder setAudioOnlySettings(AudioOnlyQuality audioQuality) {
        return this.addCommandList(audioQuality.getDownloadConversionCommands());
    }

    public LegacyCmdBuilder setPrintToFile(ResponseProperties printProperties, Path filePath) {
        if (this.printCommand != null) {
            throw new IllegalStateException("you specify only one print command");
        } else {
            this.printCommand = List.of("--print-to-file",
                printProperties.getPattern(),
                filePath.toAbsolutePath().toString());
            return this;
        }
    }

    public LegacyCmdBuilder setOutputCommand(StorageOptions storageOptions, DownloadOption downloadOption) {
        if (this.outputCommand != null) {
            throw new IllegalStateException("you specify only one output path");
        } else {
            this.outputCommand = resolveStorageOutputPatterns(storageOptions, downloadOption);
            return this;
        }
    }

    public LegacyCmdBuilder setFFMpegPath(String ffmpegCommandPath) {
        return this.addParametrizedCommand("--ffmpeg-location", ffmpegCommandPath);
    }

    public LegacyCmdBuilder setOptionalFFMpegPath(String ffmpegCommandPath, boolean needsFFmpeg) {
        if (needsFFmpeg) return this.setFFMpegPath(ffmpegCommandPath);
        else return this;
    }


    public LegacyCmdBuilder setDateAfter(LocalDateTime afterDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = afterDate.format(formatter);
        return this.addParametrizedCommand("--dateafter", date);
    }

    public LegacyCmdBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public ProcessBuilder buildReadOnlyProcess() {
        if (url == null) {
            throw new IllegalStateException("you must specify an url");
        } else if (printCommand == null) {
            throw new IllegalStateException("you must specify an print command");
        } else {
            addCommandList(printCommand);
            addSingleCommand(url);
            if (printToConsole) System.out.println(String.join(" ", this.commandList));
            if (logWithLogger && commandLogger != null) commandLogger.accept(String.join(" ", this.commandList));
            return new ProcessBuilder(this.commandList);
        }
    }

    public ProcessBuilder buildDownloadProcess() {
        if (url == null) {
            throw new IllegalStateException("you must specify an url");
        } else if (outputCommand == null) {
            throw new IllegalStateException("you must specify an output path");
        } else {
            if (printCommand != null) addCommandList(printCommand);
            addCommandList(outputCommand);
            addSingleCommand(url);
            if (printToConsole) System.out.println(String.join(" ", this.commandList));
            if (logWithLogger && commandLogger != null) commandLogger.accept(String.join(" ", this.commandList));
            return new ProcessBuilder(this.commandList);
        }
    }

    private static List<String> resolveStorageOutputPatterns(StorageOptions storageOptions, DownloadOption downloadOption) {
        String mediaTemplate = OutputPatternResolver.buildMedia(storageOptions, downloadOption);
        String metadataTemplate = OutputPatternResolver.buildMetadata(storageOptions, downloadOption);

        return switch (storageOptions.additionalFiles()){
            case MEDIA_ONLY -> List.of(
                "--output", mediaTemplate
            );
            case MEDIA_WITH_DESCRIPTION -> List.of(
                "--output", mediaTemplate,
                "--output", AdditionalFiles.FileType.DESCRIPTION.getOutputTag() + metadataTemplate
            );
            case MEDIA_WITH_METADATA -> List.of(
                "--output", mediaTemplate,
                "--output", AdditionalFiles.FileType.DESCRIPTION.getOutputTag() + metadataTemplate,
                "--output", AdditionalFiles.FileType.INFO_JSON.getOutputTag() + metadataTemplate
            );
        };
    }
}