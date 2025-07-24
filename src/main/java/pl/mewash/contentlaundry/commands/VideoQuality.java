package pl.mewash.contentlaundry.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.mewash.contentlaundry.models.general.enums.Formats;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum VideoQuality implements DownloadOption {
    MAXIMUM("2160", "MP4 up to 4K"),
    HIGH("1440", "MP4 up to 1440p)"),
    STANDARD("1080", "MP4 up to 1080p)"),
//        COMPACT("720"),
//        SMALL("480"),
//        ULTRA_SMALL("360")
    ;
    @Getter final String resolution;
    @Getter final String buttonTitle;

    public List<String> getDownloadCommand() {
        String parameter = switch (this) {
            case MAXIMUM, HIGH, STANDARD -> bestVideoAudioQualityWithFallbacks();
//                case COMPACT -> null;
//                case SMALL -> null;
//                case ULTRA_SMALL -> null;
        };
        return List.of("-f", parameter);
    }

    private String bestVideoAudioQualityWithFallbacks() {
        return Arrays.stream(VideoQuality.HighQualityVideoAudioSettings.values())
                .map(fallback -> fallback.withResolution(this.resolution))
                .collect(Collectors.joining("/"));
    }

    @Override
    public String getFormatExtension() {
        return Formats.MP4.getExtension();
    }

    @Override
    public String getShortDescription() {
        return "Video: MP4 with best quality up to  " + this.getResolution();
    }

    @Override
    public String toString() {
        return this.buttonTitle;
    }

    @AllArgsConstructor
    enum HighQualityVideoAudioSettings {
        FORCE_AAC_AUDIO("bestvideo[height<=%s][ext=mp4]+bestaudio[acodec^=mp4a]"),
        //        FORCE_OPUS_AUDIO("bestvideo[height<=%s]+bestaudio[acodec^=opus]"),  // FIXME: disable until conversion rules get added with ffprobe
        ANY_BEST_AUDIO("bestvideo[height<=%s]+bestaudio"),
        MERGED_MP4("best[height<=%s][ext=mp4]"),
        ANY_BEST_VIDEO("best[height<=%s]");
        final String placeholder;

        private String withResolution(String resolution) {
            return String.format(placeholder, resolution);
        }
    }




}