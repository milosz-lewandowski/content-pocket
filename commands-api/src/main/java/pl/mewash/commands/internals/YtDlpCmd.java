package pl.mewash.commands.internals;

import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

public interface YtDlpCmd {
    String getCommand();
    boolean isParametrized();

    interface CmdBundle {
        LinkedList<CmdEntry> getEntries();
    }
}


@RequiredArgsConstructor
enum FetchCmd implements YtDlpCmd {
    PRINT_TO_FILE("--print-to-file", true),

    SKIP_DOWNLOAD("--skip-download", false),
    PLAYLIST_END("--playlist-end", true), // =1; =7000
    QUIET_PROCESS_OUTPUT("--quiet", false),

    DATE_AFTER_FILTER("--dateafter", true),
    MATCH_FILTER("--match-filter", true), // =!is_live
    BREAK_ON_REJECT("--break-on-reject", false),

    ;
    @Getter private final String command;
    @Getter private final boolean parametrized;

    @RequiredArgsConstructor
    enum Bundle implements CmdBundle {
        FETCH_BASIC_CHANNEL_DATA(List.of(
            CmdEntry.of(FetchCmd.SKIP_DOWNLOAD),
            CmdEntry.withParam(FetchCmd.PLAYLIST_END, "1"),
            CmdEntry.of(FetchCmd.QUIET_PROCESS_OUTPUT)
        )),
        FETCH_CONTENTS_SKIP_LIVES(List.of(
            CmdEntry.of(FetchCmd.SKIP_DOWNLOAD),
            CmdEntry.withParam(FetchCmd.MATCH_FILTER, "!is_live"),
            CmdEntry.withParam(FetchCmd.PLAYLIST_END, "7000"),
            CmdEntry.of(FetchCmd.BREAK_ON_REJECT)
        )),
        ;
        private final List<CmdEntry> cmdEntries;

        public LinkedList<CmdEntry> getEntries() {
            return new LinkedList<>(cmdEntries);
        }

        public static List<CmdEntry> fetchContentsDateAfter(LocalDateTime dateAfter) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String date = dateAfter.format(formatter);
            LinkedList<CmdEntry> entries = FETCH_CONTENTS_SKIP_LIVES.getEntries();
            entries.addLast(CmdEntry.withParam(FetchCmd.DATE_AFTER_FILTER, date));
            return entries;
        }
    }
}

@RequiredArgsConstructor
enum DownloadCmd implements YtDlpCmd {

    FFMPEG_LOCATION("--ffmpeg-location", true),

    FALLBACKS_CHAIN("-f", true),
    MERGE_OUTPUT("--merge-output-format", true), // =mp4
    AUDIO_FORMAT("--audio-format", true), // =aac
    AUDIO_QUALITY("--audio-quality", true), // =0; =7

    EXTRACT_AUDIO("--extract-audio", false),
    NO_PARTIAL_FILES("--no-part", false),
    NO_POST_OVERWRITES("--no-post-overwrites", false),

    OUTPUT_TEMPLATE("--output", true),
    WRITE_DESC_FILE("--write-description", false),
    WRITE_INFO_JSON_FILE("--write-info-json", false),
    EMBED_THUMBNAIL("--embed-thumbnail", false),
    ENRICH_FILE_WITH_METADATA("--add-metadata", false),
    ;
    @Getter private final String command;
    @Getter private final boolean parametrized;

    @RequiredArgsConstructor
    enum Bundle implements CmdBundle{
        EMBED_PICTURE_AND_METADATA(List.of(
            CmdEntry.of(DownloadCmd.ENRICH_FILE_WITH_METADATA),
            CmdEntry.of(DownloadCmd.EMBED_THUMBNAIL)
        )),
        MERGE_TO_MP4_EXTRACT_BA_AAC(List.of(
            CmdEntry.withParam(DownloadCmd.MERGE_OUTPUT, "mp4"),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "aac"),
            CmdEntry.withParam(DownloadCmd.AUDIO_QUALITY, "0")
        ))
        ;
        private final List<CmdEntry> cmdEntries;

        public LinkedList<CmdEntry> getEntries() {
            return new LinkedList<>(cmdEntries);
        }

    }
}