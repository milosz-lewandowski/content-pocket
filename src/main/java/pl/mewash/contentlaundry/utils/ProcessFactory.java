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
//    private static final Path TOOL_PATH_MAC = Paths.get(System.getProperty("user.dir"), "tools", "mac", "yt-dlp_macos");
//    private static final Path TOOL_PATH_MAC = Paths.get("../Resources/tools/mac/yt-dlp_macos");
//    private static final String TOOL_COMMAND_MAC = TOOL_PATH_MAC.toString();

    private static final Path TOOL_PATH = Paths.get(BinariesContext.getToolsDir(), "yt-dlp_macos");
    private static final String TOOL_COMMAND = TOOL_PATH.toString();


    public static ProcessBuilder buildFetchUploadListCommand(String channelUrl, LocalDateTime afterDate, File tempFile) {
        List<String> command = new ArrayList<>();
        command.add(TOOL_COMMAND);

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
                TOOL_COMMAND,
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

//    public static void checkTool(String toolName) {
//        try {
//            String toolExe = toolName + ".exe";
//            String toolPathCommand = Paths.get(System.getProperty("user.dir"), "tools", toolExe).toString();
//            ProcessBuilder builder = new ProcessBuilder(toolPathCommand, "-version");
//            builder.redirectErrorStream(true);
//            Process process = builder.start();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//            process.waitFor();
//        } catch (Exception e) {
//            System.err.println("error while checking: " + toolName);
//            System.err.println(e.getMessage());
//            e.printStackTrace();
//        }
//    }
}

//    @Deprecated
//    public static ProcessBuilder getWithFormat(String url, Formats format) {
//        return switch (format) {
//            case MP3 -> getMP3proc(url);
//            case WAV -> getWAVproc(url);
//            case MP4 -> getMP4proc(url);
//        };
//    }
//
//    @Deprecated
//    private static ProcessBuilder getMP3proc(String url) {
//        Formats format = Formats.MP3;
//        return new ProcessBuilder(
//                TOOL_COMMAND,
//                "--extract-audio",
//                "--audio-format", format.value,
//                "--audio-quality", "0",
//                "--embed-thumbnail",
//                "--add-metadata",
//                "--write-description",
//                "--write-info-json",
//                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
//                url);
//    }
//
//    @Deprecated
//    private static ProcessBuilder getWAVproc(String url) {
//        Formats format = Formats.WAV;
//        return new ProcessBuilder(
//                TOOL_COMMAND,
//                "--extract-audio",
//                "--audio-format", format.value,
//                "--audio-quality", "0",
//                "--add-metadata",
//                "--write-description",
//                "--write-info-json",
//                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
//                url);
//    }
//
//    @Deprecated
//    private static ProcessBuilder getMP4proc(String url) {
//        Formats format = Formats.MP4;
//        return new ProcessBuilder(
//                TOOL_COMMAND,
//                "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/mp4",
//                "--merge-output-format", format.value,
//                "--embed-thumbnail",
//                "--add-metadata",
//                "--write-description",
//                "--write-info-json",
//                "--output", format.name() + "/%(title)s/%(title)s.%(ext)s",
//                url
//        );
//    }
//}