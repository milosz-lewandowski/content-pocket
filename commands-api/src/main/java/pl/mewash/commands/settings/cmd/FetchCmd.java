package pl.mewash.commands.settings.cmd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.api.entries.CmdEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public enum FetchCmd implements DlpCmd {
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
    public enum Bundle implements CmdBundle {
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
