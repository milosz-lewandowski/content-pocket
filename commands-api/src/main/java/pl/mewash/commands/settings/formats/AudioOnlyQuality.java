package pl.mewash.commands.settings.formats;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@AllArgsConstructor
public enum AudioOnlyQuality implements DownloadOption {

    // TODO: prepare more specific download fallbacks and conversion rules based on ffprobe metrics

    MP3("mp3", "mp3", "",
        "MP3 (Good Quality)",
            true, true, "MP3 up to 320 kbps",
            List.of(
                    "-f", """
                    bestaudio[abr>=128][acodec^=opus]/\
                    bestaudio[abr>=128][acodec^=m4a]/\
                    bestaudio""",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "0"
            )),
    M4A("m4a high quality", "m4a_HQ", "(HQ)",
        "M4A (High Quality)",
            true, true, "M4A (AAC codec) up to 320 kbps",
            List.of(
                    "-f", """
                    bestaudio[acodec^=m4a][abr>=160]/\
                    bestaudio[acodec^=opus]/\
                    bestaudio[acodec^=m4a]/\
                    bestaudio""",
                    "--extract-audio",
                    "--audio-format", "m4a",
                    "--audio-quality", "0"
            )),
    M4A_SMALL_SIZE("m4a small size", "m4a_SmSize", "(SmSize)",
        "M4A (Small Size)",
            true, true, "M4A (AAC codec) up to 160 kbps in",
            List.of(
                    "-f", """
                    bestaudio[acodec^=m4a][abr<=160]/\
                    bestaudio[acodec^=m4a][abr>=128]/\
                    bestaudio[acodec^=opus]/\
                    bestaudio[acodec^=m4a]/\
                    bestaudio""",
                    "--extract-audio",
                    "--audio-format", "m4a",
                    "--audio-quality", "5"
            )),
    WAV("wav", "wav",  "",
        "WAV (Converted from lossy)",
            true, false, "WAV (may cause lossy conversion from original codec)",
            List.of(
                    "-f", """
                    bestaudio[acodec^=opus]/\
                    bestaudio[acodec^=m4a]/\
                    bestaudio""",
                    "--extract-audio",
                    "--audio-format", "wav"
            )),
    ORIGINAL_SOURCE("original source codec", "original", "(as original)",
        "Original Codec (Best Source Quality)",
            false, false, "Original source codec (no lossy conversion)",
            List.of(
                    "-f", """
                    bestaudio[abr>=96][acodec^=opus]/\
                    bestaudio[abr>=96][acodec^=m4a]/\
                    bestaudio""",
                    "--no-part",
                    "--no-post-overwrites"
            ));

    @Getter private final String extension;

    @Getter private final String dirName;
    @Getter private final String titleDiff;

    @Getter private final String buttonTitle;
    private final boolean useFFmpeg;
    private final boolean canEmbedMetadata;
    private final String description;
    private final List<String> conversionCommands;

    @Getter final static Predicate<Set<DownloadOption>> conflictsPredicate = (options) -> options
        .contains(AudioOnlyQuality.M4A) && options
        .contains(AudioOnlyQuality.M4A_SMALL_SIZE);

    public List<String> getDownloadConversionCommands() {
        return new ArrayList<>(this.conversionCommands);
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