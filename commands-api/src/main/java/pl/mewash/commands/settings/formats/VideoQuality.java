package pl.mewash.commands.settings.formats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.cmd.DownloadCmd;
import pl.mewash.commands.api.entries.CmdEntry;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum VideoQuality implements VideoOption {
    MAXIMUM("2160", "MP4 up to 4K", "mp4", "(as 4K)"),
    HIGH("1440", "MP4 up to 1440p", "mp4", "(as 1440p)"),
    STANDARD("1080", "MP4 (H.264) up to 1080p", "mp4", "(as 1080p)"),
    COMPACT("720", "MP4 (H.264) up to 720p", "mp4", "(as 720p)"),
    ;

    @Getter private final String resolution;
    @Getter private final String buttonTitle;

    @Getter private final String dirName;
    @Getter private final String titleDiff;

    @Getter final static Predicate<Set<DownloadOption>> conflictsPredicate = (options) -> options
        .stream()
        .filter(dOption -> dOption instanceof VideoQuality)
        .count() > 1;

    public List<String> getDownloadCommand() {
        return List.of("-f", getCommandResolvedResolution());
    }

    public List<CmdEntry> getCmdEntries() {
        return new LinkedList<>(List.of(
            CmdEntry.withParam(DownloadCmd.FALLBACKS_CHAIN, getCommandResolvedResolution())));
    }

    private String getCommandResolvedResolution() {
        return switch (this) {
            case STANDARD, COMPACT -> getFallbacksChain(true);
            case MAXIMUM, HIGH -> getFallbacksChain(false);
        };
    }

    private String getFallbacksChain(boolean forceH264) {
        List<StreamFallbacks> fallbacks = forceH264
            ? StreamFallbacks.getEnforceH264Fallbacks()
            : StreamFallbacks.getAnyBvFallbacks();
        return fallbacks.stream()
            .map(fallback -> fallback.withResolution(this.resolution))
            .collect(Collectors.joining("/"));
    }

    @Override
    public String getOptionName() {
        return resolution + "p " + dirName;
    }

    @Override
    public String toString() {
        return this.buttonTitle;
    }

    @RequiredArgsConstructor
    public enum StreamFallbacks {
        FORCE_H264_BV_AAC_BA(true, "bestvideo[height<=%s][vcodec*=avc1]+bestaudio[acodec^=mp4a]"),
        FORCE_H264_BV_ANY_BA(true, "bestvideo[height<=%s][vcodec*=avc1]+bestaudio"),
        FORCE_H264_ANY_RES_V_AAC_AUDIO(true, "bestvideo[vcodec*=avc1]+bestaudio[acodec^=mp4a]"),
        FORCE_H264_ANY_RES_V_ANY_BA(true, "bestvideo[vcodec*=avc1]+bestaudio"),

        MERGED_BV_AAC_BA(false, "bestvideo[height<=%s][ext=mp4]+bestaudio[acodec^=mp4a]"),
        ANY_BV_AAC_BA(false, "bestvideo[height<=%s]+bestaudio[acodec^=mp4a]"),
        ANY_BV_ANY_BA(false, "bestvideo[height<=%s]+bestaudio"),
        BEST_MERGED_MP4(false, "best[height<=%s][ext=mp4]"),
        ANY_BEST_WITH_RES(false, "best[height<=%s]"),
        ANY_RES_BV_AAC_BA(false, "bestvideo+bestaudio[acodec^=mp4a]"),
        ANY_RES_ANY_BEST(false, "best");


        private final boolean enforcesH264;
        private final String template;

        private String withResolution(String resolution) {
            return String.format(template, resolution);
        }

        public static List<StreamFallbacks> getEnforceH264Fallbacks() {
            return Arrays.stream(values())
                .filter(fallback -> fallback.enforcesH264)
                .toList();
        }

        public static List<StreamFallbacks> getAnyBvFallbacks() {
            return Arrays.stream(values())
                .filter(fallback -> !fallback.enforcesH264)
                .toList();
        }
    }
}