package pl.mewash.commands.settings.formats;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public enum AudioOnlyQuality implements DownloadOption {

    // TODO: prepare more specific download fallbacks and conversion rules based on ffprobe metrics

    MP3(Formats.MP3, "MP3 (Good Quality)",
            true, true, "MP3 up to 320 kbps", List.of(
            "-f", "bestaudio[abr>=128][acodec^=opus]/bestaudio[abr>=128][acodec^=m4a]/bestaudio",
            "--extract-audio",
            "--audio-format", "mp3",
            "--audio-quality", "0"
    )),
    M4A(Formats.M4A, "M4A (High Quality)",
            true, true, "M4A (AAC codec) up to 320 kbps", List.of(
            "-f", "bestaudio[acodec^=m4a][abr>=160]/bestaudio[acodec^=opus]/bestaudio[acodec^=m4a]/bestaudio",
            "--extract-audio",
            "--audio-format", "m4a",
            "--audio-quality", "0"
    )),
    M4A_COMPRESSED(Formats.M4A, "M4A (Compressed)",
            true, true, "M4A (AAC codec) up to 160 kbps in", List.of(
            "-f", "bestaudio[acodec^=m4a][abr<=160]/bestaudio[acodec^=m4a][abr>=128]/bestaudio[acodec^=opus]/bestaudio[acodec^=m4a]/bestaudio",
            "--extract-audio",
            "--audio-format", "m4a",
            "--audio-quality", "5"
    )),
    WAV(Formats.WAV, "WAV (Converted from lossy)",
            true, false, "WAV (may cause lossy conversion from original codec)", List.of(
            "-f", "bestaudio[acodec^=opus]/bestaudio[acodec^=m4a]/bestaudio",
            "--extract-audio",
            "--audio-format", "wav"
    )),
    ORIGINAL_SOURCE(Formats.ORIGINAL_SOURCE, "Original Codec (Best Source Quality)",
            false, false, "Original source codec (no lossy conversion)", List.of(
            "-f", "bestaudio[abr>=96][acodec^=opus]/bestaudio[abr>=96][acodec^=m4a]/bestaudio",
            "--no-part",
            "--no-post-overwrites"
    ));

    final Formats formats;
    @Getter final String buttonTitle;
    final boolean useFFmpeg;
    final boolean canEmbedMetadata;
    final String description;
    final List<String> conversionCommands;


    public List<String> getDownloadConversionCommands() {
        return new ArrayList<>(this.conversionCommands);
    }

    public boolean needsFFmpeg() {
        return this.useFFmpeg;
    }

    public Formats getFormat() {
        return this.formats;
    }

    public boolean getCanEmbedMetadata() {
        return canEmbedMetadata;
    }


    @Override
    public String getFormatExtension() {
        return this.formats.getExtension();
    }

    @Override
    public String getShortDescription() {
        return "Audio: " + this.description;
    }


    @Override
    public String toString() {
        return this.buttonTitle;
    }
}
//    @AllArgsConstructor
//    enum FFMpegCommands{
//        MP3(List.of(
//                "-i", "<input>",
//                "-vn",                        // no video
//                "-c:a", "libmp3lame",         // MP3 codec
//                "-q:a", "0",                  // highest quality VBR (≈245–320kbps)
//                "<output>"
//        )),
//
//        AAC_HIGHEST(List.of(
//                "-i", "<input>",
//                "-vn",
//                "-c:a", "aac",                // built-in AAC encoder (good compatibility)
//                "-b:a", "256k",               // high quality bitrate (≈256kbps)
//                "<output>"
//        )),
//
//        AAC_COMPACT(List.of(
//                "-i", "<input>",
//                "-vn",
//                "-c:a", "aac",
//                "-b:a", "128k",               // compact profile (≈128kbps, small files)
//                "<output>"
//        )),
//
//        WAV(List.of(
//                "-i", "<input>",
//                "-vn",
//                "-c:a", "pcm_s16le",          // standard uncompressed WAV codec
//                "<output>"
//        )),
//
//        SOURCE_BEST(List.of(
//                "-i", "<input>",
//                "-vn",
//                "-c:a", "copy",               // no re-encoding = lossless
//                "<output>"
//        ));
//
//        private List<String> commands;
//    }