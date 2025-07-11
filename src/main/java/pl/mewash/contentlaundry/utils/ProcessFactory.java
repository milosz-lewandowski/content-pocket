package pl.mewash.contentlaundry.utils;

import pl.mewash.contentlaundry.BinariesContext;
import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.Formats;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProcessFactory {

    private static final String TOOLS_DIR = BinariesContext.getToolsDir();
    private static final String COMMAND_YT_DLP = Paths.get(TOOLS_DIR, "yt-dlp_macos").toAbsolutePath().toString();
    private static final String COMMAND_FFMPEG = Paths.get(TOOLS_DIR, "ffmpeg").toAbsolutePath().toString();

    public static ProcessBuilder buildFetchUploadListCommand(String channelUrl, LocalDateTime afterDate, File tempFile) {
        List<String> command = new ArrayList<>();
        command.add(COMMAND_YT_DLP);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = afterDate.format(formatter);

        command.addAll(List.of(
                "--dateafter", date,
                "--skip-download",
                "--match-filter", "!is_live",
                "--break-on-reject",
//                "--playlist-items", "15", // don't use - does not work as expected
                "--playlist-end", "150", // Safety for infinite dangling process
                "--quiet",  // Optional: suppress progress bars
                "--print-to-file", "%(upload_date)s ||| %(title)s ||| %(webpage_url)s ||| %(id)s", tempFile.getAbsolutePath(),
                channelUrl
        ));
        System.out.println(command);

        return new ProcessBuilder(command);
    }

    public static ProcessBuilder buildCheckChannelCommand(String channelUrl, File tempFile) {
        List<String> command = new ArrayList<>(List.of(
                COMMAND_YT_DLP,
                "--skip-download",
                "--playlist-end", "1",
                "--quiet",
//                "--print", "%(channel)s",
                "--print-to-file", "%(channel)s", tempFile.getAbsolutePath(),
                channelUrl
        ));

        return new ProcessBuilder(command);


    }

    public static ProcessBuilder buildDownloadCommand(String url, Formats format,
                                                      AdvancedOptions advancedOptions, Path tempTitleFile) {
        List<String> command = new ArrayList<>();
        command.add(COMMAND_YT_DLP); // tool command

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

        command.addAll(List.of("--ffmpeg-location", COMMAND_FFMPEG)); // explicitly pass path to ffmpeg

        // adds metadata files (.info.json + .description)
        if (advancedOptions.withMetadata()) {
            command.addAll(List.of(
                    "--write-description",
                    "--write-info-json"
            ));
        }

        command.addAll(List.of("--print-to-file", "%(title)s", tempTitleFile.toAbsolutePath().toString()));

        String outputPathString = getOutputPathParam(advancedOptions, format);
        command.addAll(List.of(
                "--output", outputPathString,
                url
        ));

        System.out.println("mp4 debug command: " + command);
        return new ProcessBuilder(command);
    }

    private static String getOutputPathParam(AdvancedOptions advancedOptions, Formats format) {
        String formatDir = format.name() + "/";
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