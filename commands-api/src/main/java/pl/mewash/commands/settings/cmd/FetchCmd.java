package pl.mewash.commands.settings.cmd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.api.entries.CmdEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public enum FetchCmd implements DlpCmd {
    PRINT_TO_FILE("--print-to-file", true),

    SKIP_DOWNLOAD("--skip-download", false),
    PLAYLIST_END("--playlist-end", true),
    QUIET_PROCESS_OUTPUT("--quiet", false),

    LAZY_PLAYLIST("--lazy-playlist", false),
    SLEEP_REQUESTS("--sleep-requests", true),

    MATCH_FILTER("--match-filter", true),
    // Deprecated because of: yt-dlp#11046 – “process never exits when using --break-on-reject in playlist mode”
    // instead use: --break-match-filters
    @Deprecated DATE_AFTER_FILTER("--dateafter", true),
    @Deprecated BREAK_ON_REJECT("--break-on-reject", false),
    BREAK_MATCH_FILTERS("--break-match-filters", true),

    EXTRACTOR_ARGS("--extractor-args", true)
    ;

    @Getter private final String command;
    @Getter private final boolean parametrized;

    @RequiredArgsConstructor
    public enum Bundle implements CmdBundle {
        FETCH_BASIC_CHANNEL_DATA(List.of(
            CmdEntry.of(FetchCmd.SKIP_DOWNLOAD),
            CmdEntry.withParam(FetchCmd.PLAYLIST_END, "1"),
            CmdEntry.of(FetchCmd.QUIET_PROCESS_OUTPUT)
        )),
        @Deprecated
        FETCH_CONTENTS_SKIP_LIVES(List.of(
            CmdEntry.of(FetchCmd.SKIP_DOWNLOAD),
            CmdEntry.withParam(FetchCmd.MATCH_FILTER, "!is_live"),
            CmdEntry.withParam(FetchCmd.PLAYLIST_END, "7000"),
            CmdEntry.of(FetchCmd.BREAK_ON_REJECT)
        )),

        FETCH_CONTENTS_SKIP_LIVES_NO_BREAK(List.of(
            CmdEntry.of(FetchCmd.SKIP_DOWNLOAD),
            CmdEntry.withParam(FetchCmd.MATCH_FILTER, "!is_live")
        )),
        ;
        private final List<CmdEntry> cmdEntries;

        public LinkedList<CmdEntry> getEntries() {
            return new LinkedList<>(cmdEntries);
        }

        @Deprecated // use 'getFetchContentsBreakAfter' instead
        public static List<CmdEntry> getFetchContentsDateAfterEntries(LocalDateTime dateAfter) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String date = dateAfter.format(formatter);
            LinkedList<CmdEntry> entries = FETCH_CONTENTS_SKIP_LIVES.getEntries();
            entries.addLast(CmdEntry.withParam(FetchCmd.DATE_AFTER_FILTER, date));
            return entries;
        }

        public static List<CmdEntry> getFetchContentsBreakAfter(LocalDateTime dateAfter) {
            LinkedList<CmdEntry> entries = FETCH_CONTENTS_SKIP_LIVES_NO_BREAK.getEntries();
            entries.add(CmdEntry.withParam(PLAYLIST_END, calculatePlaylistEndParam(dateAfter, 10)));
            entries.add(CmdEntry.withParam(EXTRACTOR_ARGS, "youtube:player_client=android"));

            // limit rate
            entries.add(CmdEntry.of(FetchCmd.LAZY_PLAYLIST));
            entries.add(CmdEntry.withParam(FetchCmd.SLEEP_REQUESTS, "2"));

            String date = dateAfter.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String breakFilter = String.format("upload_date>=%s", date);
            entries.addLast(CmdEntry.withParam(FetchCmd.BREAK_MATCH_FILTERS, breakFilter));

            return entries;
        }

        private static String calculatePlaylistEndParam(LocalDateTime dateAfter, int contentsPerDay) {
            long daysSinceDate = ChronoUnit.DAYS.between(dateAfter, LocalDateTime.now());
            return String.valueOf(Math.max(50, daysSinceDate * contentsPerDay));
        }
    }
}
