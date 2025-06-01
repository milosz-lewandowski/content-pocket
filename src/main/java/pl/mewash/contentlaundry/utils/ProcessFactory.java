package pl.mewash.contentlaundry.utils;

import pl.mewash.contentlaundry.models.AdvancedOptions;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProcessFactory {
    private static final Path TOOL_PATH = Paths.get(System.getProperty("user.dir"), "tools", "yt-dlp.exe");
    private static final String TOOL_COMMAND = TOOL_PATH.toString();

    public static ProcessBuilder buildProcessCommand(String url, Formats format, AdvancedOptions advancedOptions) {
        List<String> command = new ArrayList<>();
        command.add(TOOL_COMMAND); // tool command

        // adds format specific download & conversion options
        if (format.audioFormat) {
            command.addAll(List.of(
                    "--extract-audio",
                    "--audio-format", format.value,
                    "--audio-quality", "0"
            ));
        } else if (format == Formats.MP4) {
            command.addAll(List.of(
                    "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4",
                    "--merge-output-format", format.value
            ));
        } else System.err.println("Unsupported format: " + format);

        if (format != Formats.WAV) {
            command.add("--embed-thumbnail");    // adds picture miniature to content file
            command.add("--add-metadata");       // enrich content file with metadata (no full support for WAV)
        }

        // adds metadata files (.info.json + .description)
        if (advancedOptions.withMetadata()) {
            command.addAll(List.of(
                    "--write-description",
                    "--write-info-json"
            ));
        }

        String outputPath = getOutputPath(advancedOptions, format);

        command.addAll(List.of(
                "--output", outputPath,
                url
        ));

        return new ProcessBuilder(command);
    }

    private static String getOutputPath(AdvancedOptions advancedOptions, Formats format) {
        String formatDir = format.name() + "/";
        String fileTitleWithExtension = "%(title)s.%(ext)s";
        String titleDir = "%(title)s/";
        String titleDirWithExtension = "%(title)s-" + formatDir;
        String dateDir = advancedOptions.withDateDir() ? LocalDate.now() + "/" : "";


        return switch (advancedOptions.outputStructure()) {

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

    @Deprecated
    public static ProcessBuilder getWithFormat(String url, Formats format) {
        return switch (format) {
            case MP3 -> getMP3proc(url);
            case WAV -> getWAVproc(url);
            case MP4 -> getMP4proc(url);
        };
    }

    @Deprecated
    private static ProcessBuilder getMP3proc(String url) {
        Formats format = Formats.MP3;
        return new ProcessBuilder(
                TOOL_COMMAND,
                "--extract-audio",
                "--audio-format", format.value,
                "--audio-quality", "0",
                "--embed-thumbnail",
                "--add-metadata",
                "--write-description",
                "--write-info-json",
                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
                url);
    }

    @Deprecated
    private static ProcessBuilder getWAVproc(String url) {
        Formats format = Formats.WAV;
        return new ProcessBuilder(
                TOOL_COMMAND,
                "--extract-audio",
                "--audio-format", format.value,
                "--audio-quality", "0",
                "--add-metadata",
                "--write-description",
                "--write-info-json",
                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
                url);
    }

    @Deprecated
    private static ProcessBuilder getMP4proc(String url) {
        Formats format = Formats.MP4;
        return new ProcessBuilder(
                TOOL_COMMAND,
                "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4",
                "--merge-output-format", format.value,
                "--embed-thumbnail",
                "--add-metadata",
                "--write-description",
                "--write-info-json",
                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
                url
        );
    }
}