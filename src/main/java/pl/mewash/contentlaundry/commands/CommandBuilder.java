package pl.mewash.contentlaundry.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.mewash.contentlaundry.AppContext;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.utils.ScheduledFileLogger;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {

    private static final String COMMAND_YT_DLP = AppContext.getInstance().getYtDlpCommand();
    private static final String COMMAND_FFMPEG = AppContext.getInstance().getFfMpegCommand();

    private final List<String> commandList;
    private List<String> outputCommand;
    private List<String> printCommand;
    private String url;
    private boolean logToConsole = false;
    private boolean logToFile = false;

    private CommandBuilder() {
        this.commandList = new ArrayList<>();
    }

    public static CommandBuilder newYtDlpCommand() {
        return new CommandBuilder().addSingleCommand(COMMAND_YT_DLP);
    }

    public CommandBuilder addSingleCommand(String command) {
        this.commandList.add(command);
        return this;
    }

    public CommandBuilder logCommandToConsole() {
        this.logToConsole = true;
        return this;
    }

    public CommandBuilder logCommandToFile() {
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

    public CommandBuilder setPrintToFile(PrintToFileOptions option, Path filePath) {
        if (this.printCommand != null) {
            throw new IllegalStateException("you specify only one print command");
        } else {
            this.printCommand = List.of("--print-to-file",
                    option.getValue(),
                    filePath.toAbsolutePath().toString());
            return this;
        }
    }

    public CommandBuilder setOutputCommand(AdvancedOptions advancedOptions, Formats format) {
        if (this.outputCommand != null) {
            throw new IllegalStateException("you specify only one output path");
        } else {
            String formattedOutputPath = getOutputPathParam(advancedOptions, format);
            this.outputCommand = List.of("--output", formattedOutputPath);
            return this;
        }
    }

    public CommandBuilder setFFMpegPath() {
        return this.addParametrizedCommand("--ffmpeg-location", COMMAND_FFMPEG);
    }

    public CommandBuilder setOptionalFFMpegPath(boolean needsFFmpeg) {
        if (needsFFmpeg) return this.setFFMpegPath();
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
            if (logToConsole) System.out.println(String.join(" ", this.commandList));
            if (logToFile) ScheduledFileLogger.appendSingleLine(String.join(" ", this.commandList));
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
            if (logToConsole) System.out.println(String.join(" ", this.commandList));
            if (logToFile) ScheduledFileLogger.appendSingleLine(String.join(" ", this.commandList));
            return new ProcessBuilder(this.commandList);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum PrintToFileOptions {
        CHANNEL_NAME("%(channel)s", null),
        CHANNEL_NAME_LATEST_CONTENT("%(channel)s ||| %(upload_date)s", "\\|\\|\\|"),
        CONTENT_TITLE("%(title)s", null),
        CONTENT_PROPERTIES("%(upload_date)s ||| %(title)s ||| %(webpage_url)s ||| %(id)s", "\\|\\|\\|");
        final String value;
        final String splitRegex;
    }


    private static String getOutputPathParam(AdvancedOptions advancedOptions, Formats format) {
        String formatDir = format.fileExtension + "/";
        String fileTitleWithExtension = "%(title)s.%(ext)s";
        String titleDir = "%(title)s/";
        String titleDirWithExtension = "%(title)s-" + formatDir;
        String dateDir = advancedOptions.withDateDir() ? LocalDate.now() + "/" : "";

        return switch (advancedOptions.groupingMode()) {

            case GROUP_BY_FORMAT -> advancedOptions.withMetadata()
                    ? formatDir + dateDir + titleDir + fileTitleWithExtension
                    : formatDir + dateDir + fileTitleWithExtension;

            case GROUP_BY_CONTENT -> advancedOptions.withMetadata()
                    ? dateDir + titleDir + titleDirWithExtension + fileTitleWithExtension
                    : dateDir + titleDir + fileTitleWithExtension;

            case NO_GROUPING -> advancedOptions.withMetadata()
                    ? dateDir + titleDirWithExtension + fileTitleWithExtension
                    : dateDir + fileTitleWithExtension;
        };
    }
}