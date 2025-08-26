package pl.mewash.commands.internals;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.formats.DownloadOption;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public enum OnlyAudioStream implements DownloadOption {

    // TODO: prepare more specific download fallbacks and conversion rules based on ffprobe metrics

    MP3("mp3", "mp3", "",
        "MP3 (Good Quality)",
        true, true, "MP3 up to 320 kbps",
        List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, """
                bestaudio[abr>=128][acodec^=opus]/\
                bestaudio[abr>=128][acodec^=m4a]/\
                bestaudio"""),
            CmdEntry.of(DownloadCmd.EXTRACT_AUDIO),
            CmdEntry.withParam(DownloadCmd.AUDIO_FORMAT, "mp3"),
            CmdEntry.withParam(DownloadCmd.AUDIO_QUALITY, "0")
        )),
    M4A("m4a high quality", "m4a_hq", "(HQ)",
        "M4A (High Quality)",
        true, true, "M4A (AAC codec) up to 320 kbps",
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
    M4A_SMALL_SIZE("m4a small size", "m4a_sm_size", "(SmSize)",
        "M4A (Small Size)",
        true, true, "M4A (AAC codec) up to 160 kbps in",
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
    WAV("wav", "wav", "",
        "WAV (Converted from lossy)",
        true, false, "WAV (may cause lossy conversion from original codec)",
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
    ORIGINAL_SOURCE("original source codec", "original", "(as original)",
        "Original codec (No lossy conversion)",
        false, false, "Original source codec (no lossy conversion)",
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

    @Getter private final String extension;

    @Getter private final String dirName;
    @Getter private final String titleDiff;

    @Getter private final String buttonTitle;
    private final boolean useFFmpeg;
    private final boolean canEmbedMetadata;
    private final String description;
    private final List<CmdEntry> conversionCommands;

    @Getter final static Predicate<Set<DownloadOption>> conflictsPredicate = (options) -> options
        .contains(pl.mewash.commands.settings.formats.AudioOnlyQuality.M4A) && options
        .contains(pl.mewash.commands.settings.formats.AudioOnlyQuality.M4A_SMALL_SIZE);

    public List<CmdEntry> getDownloadConversionCommands() {
        return new LinkedList<>(this.conversionCommands);
    }

    public boolean needsFFmpeg() {
        return this.useFFmpeg;
    }

    public boolean getCanEmbedMetadata() {
        return canEmbedMetadata;
    }

    @Override
    public String getOptionName() {
        return extension;
    }

    @Override
    public String toString() {
        return this.buttonTitle;
    }
}