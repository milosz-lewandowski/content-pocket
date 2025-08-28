package pl.mewash.commands.settings.formats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.cmd.DownloadCmd;
import pl.mewash.commands.api.entries.CmdEntry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public enum AudioOnlyQuality implements DownloadOption {

    // TODO: prepare more specific download fallbacks and conversion rules based on ffprobe metrics

    MP3("mp3", "MP3 (Good Quality)",
        "", "mp3",
        true, true,
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[abr>=128][acodec^=opus]/\
                bestaudio[abr>=128][acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.EXTRACT_AUDIO),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "mp3"),
            CmdEntry.withParam(DownloadCmd.AUDIO_QUALITY, "0")
        )),
    M4A("m4a high quality", "M4A (High Quality)",
        "(HQ)", "m4a_hq",
        true, true,
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[acodec^=m4a][abr>=160]/\
                bestaudio[acodec^=opus]/\
                bestaudio[acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.EXTRACT_AUDIO),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "m4a"),
            CmdEntry.withParam(DownloadCmd.AUDIO_QUALITY, "0")
        )),
    M4A_SMALL_SIZE("m4a small size", "M4A (Small Size)",
        "(SmSize)", "m4a_sm_size",
        true, true,
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[acodec^=m4a][abr<=160]/\
                bestaudio[acodec^=m4a][abr>=128]/\
                bestaudio[acodec^=opus]/\
                bestaudio[acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.EXTRACT_AUDIO),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "m4a"),
            CmdEntry.withParam(DownloadCmd.AUDIO_QUALITY, "7")
        )),
    WAV("wav", "WAV (Converted from lossy)",
        "", "wav",
        true, false,
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[abr>160][acodec^=opus]\
                bestaudio[abr>192][acodec^=m4a]/\
                bestaudio[acodec^=opus]/\
                bestaudio[acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.EXTRACT_AUDIO),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "m4a")
        )),
    ORIGINAL_SOURCE("original source codec", "Original codec (No lossy conversion)",
        "(as original)", "original",
        false, false,
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[abr>192][acodec^=m4a]/\
                bestaudio[abr>=160][acodec^=opus]\
                bestaudio[abr>=128][acodec^=m4a]\
                bestaudio[abr>=96][acodec^=opus]/\
                bestaudio[abr>=96][acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.NO_PARTIAL_FILES),
            CmdEntry.of(DownloadCmd.NO_POST_OVERWRITES)
        ));

    @Getter private final String optionName;
    @Getter private final String buttonTitle;

    @Getter private final String dirName;
    @Getter private final String titleDiff;

    private final boolean useFFmpeg;
    private final boolean canEmbedMetadata;

    private final List<CmdEntry> conversionCommands;

    @Getter final static Predicate<Set<DownloadOption>> conflictsPredicate = (options) -> options
        .contains(AudioOnlyQuality.M4A) && options
        .contains(AudioOnlyQuality.M4A_SMALL_SIZE);


    public List<CmdEntry> getCmdEntries() {
        return new LinkedList<>(this.conversionCommands);
    }

    public List<String> getDownloadConversionCommands() {
        return new ArrayList<>(this.conversionCommands.stream()
            .flatMap(entry -> entry.mapToStringList().stream())
            .toList());
    }

    public boolean needsFFmpeg() {
        return this.useFFmpeg;
    }

    public boolean getCanEmbedMetadata() {
        return canEmbedMetadata;
    }

    @Override
    public String toString() {
        return this.buttonTitle;
    }
}