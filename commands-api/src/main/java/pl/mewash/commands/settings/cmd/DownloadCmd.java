package pl.mewash.commands.settings.cmd;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.api.entries.CmdEntry;

import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
public enum DownloadCmd implements DlpCmd {

    FFMPEG_LOCATION("--ffmpeg-location", true),

    FALLBACKS_CHAIN("-f", true),
    MERGE_OUTPUT("--merge-output-format", true), // =mp4
    AUDIO_FORMAT("--audio-format", true), // =aac
    AUDIO_QUALITY("--audio-quality", true), // =0; =7

    EXTRACT_AUDIO("--extract-audio", false),
    NO_PARTIAL_FILES("--no-part", false),
    NO_POST_OVERWRITES("--no-post-overwrites", false),

    EMBED_THUMBNAIL("--embed-thumbnail", false),
    ENRICH_FILE_WITH_METADATA("--add-metadata", false),
    WRITE_DESC_FILE("--write-description", false),
    WRITE_INFO_JSON_FILE("--write-info-json", false),

    OUTPUT_TEMPLATE("--output", true),
    ;

    @Getter private final String command;
    @Getter private final boolean parametrized;

    @RequiredArgsConstructor
    public enum Bundle implements CmdBundle{
        EMBED_PICTURE_AND_METADATA(List.of(
            CmdEntry.of(DownloadCmd.ENRICH_FILE_WITH_METADATA),
            CmdEntry.of(DownloadCmd.EMBED_THUMBNAIL)
        )),
        MERGE_TO_MP4_WITH_BA_AAC(List.of(
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
