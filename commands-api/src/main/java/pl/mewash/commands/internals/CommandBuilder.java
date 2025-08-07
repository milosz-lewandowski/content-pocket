package pl.mewash.commands.internals;

import pl.mewash.commands.api.CommandLogger;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.Formats;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ResponseProperties;
import pl.mewash.commands.settings.storage.StorageOptions;


import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    private final List<String> commandList;
    private List<String> outputCommand;
    private List<String> printCommand;
    private String url;
    private boolean printToConsole = false;

    private boolean logToFile = false;
    private CommandLogger logger;

    private CommandBuilder() {
        this.commandList = new ArrayList<>();
    }

    public static CommandBuilder newYtDlpCommand(String ytDlpCommandPath) {
        return new CommandBuilder().addSingleCommand(ytDlpCommandPath);
    }

    public CommandBuilder addSingleCommand(String command) {
        this.commandList.add(command);
        return this;
    }

    public CommandBuilder printCommandToConsole(boolean printToConsole) {
        this.printToConsole = printToConsole;
        return this;
    }

    public CommandBuilder logCommandToFile(CommandLogger logger) {
        this.logger = logger;
        this.logToFile = true;
        return this;
    }

    public CommandBuilder addParametrizedCommand(String command, String parameter) {
        this.commandList.add(command);
        this.commandList.add(parameter);
        return this;
    }

    public CommandBuilder addCommandBundle(CommandBundles bundle) {
        bundle.appendAtEndOf(this.commandList);
        return this;
    }

    public CommandBuilder addOptionalCommandBundle(CommandBundles bundle, boolean isSelected) {
        if (isSelected) bundle.appendAtEndOf(this.commandList);
        return this;
    }

    public CommandBuilder addCommandList(List<String> listOfCommands) {
        this.commandList.addAll(listOfCommands);
        return this;
    }

    public CommandBuilder setVideoQuality(VideoQuality videoQuality) {
        this.addCommandList(videoQuality.getDownloadCommand());
        return this;
    }

    public CommandBuilder setAudioOnlySettings(AudioOnlyQuality audioQuality) {
        return this.addCommandList(audioQuality.getDownloadConversionCommands());
    }

    public CommandBuilder setPrintToFile(ResponseProperties printProperties, Path filePath) {
        if (this.printCommand != null) {
            throw new IllegalStateException("you specify only one print command");
        } else {
            this.printCommand = List.of("--print-to-file",
                    printProperties.getPattern(),
                    filePath.toAbsolutePath().toString());
            return this;
        }
    }

    public CommandBuilder setOutputCommand(StorageOptions storageOptions, Formats format) {
        if (this.outputCommand != null) {
            throw new IllegalStateException("you specify only one output path");
        } else {
            String formattedOutputPath = getOutputPathParam(storageOptions, format);
            this.outputCommand = List.of("--output", formattedOutputPath);
            return this;
        }
    }

    public CommandBuilder setFFMpegPath(String ffmpegCommandPath) {
        return this.addParametrizedCommand("--ffmpeg-location", ffmpegCommandPath);
    }

    public CommandBuilder setOptionalFFMpegPath(String ffmpegCommandPath, boolean needsFFmpeg) {
        if (needsFFmpeg) return this.setFFMpegPath(ffmpegCommandPath);
        else return this;
    }


    public CommandBuilder setDateAfter(LocalDateTime afterDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = afterDate.format(formatter);
        return this.addParametrizedCommand("--dateafter", date);
    }

    public CommandBuilder setUrl(String url) {
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
            if (logToFile && logger != null) logger.log(String.join(" ", this.commandList));
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
            if (logToFile && logger != null) logger.log(String.join(" ", this.commandList));
            return new ProcessBuilder(this.commandList);
        }
    }


    private static String getOutputPathParam(StorageOptions storageOptions, Formats format) {
        String formatDir = format.getExtension() + "/";
        String fileTitleWithExtension = "%(title)s.%(ext)s";
        String titleDir = "%(title)s/";
        String titleDirWithExtension = "%(title)s-" + formatDir;
        String dateDir = storageOptions.withDownloadedDateDir() ? LocalDate.now() + "/" : "";

        return switch (storageOptions.groupingMode()) {

            case GROUP_BY_FORMAT -> storageOptions.withMetadataFiles()
                    ? formatDir + dateDir + titleDir + fileTitleWithExtension
                    : formatDir + dateDir + fileTitleWithExtension;

            case GROUP_BY_CONTENT -> storageOptions.withMetadataFiles()
                    ? dateDir + titleDir + titleDirWithExtension + fileTitleWithExtension
                    : dateDir + titleDir + fileTitleWithExtension;

            case NO_GROUPING -> storageOptions.withMetadataFiles()
                    ? dateDir + titleDirWithExtension + fileTitleWithExtension
                    : dateDir + fileTitleWithExtension;
        };
    }
}