package pl.mewash.subscriptions.internal.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;

import java.time.Period;

@Getter
@Builder
@Jacksonized
public class ChannelSettings {

    @Builder.Default private final AudioOnlyQuality defaultAudio = AudioOnlyQuality.MP3;
    @Builder.Default private final VideoQuality defaultVideo = VideoQuality.STANDARD;

    @Builder.Default private final boolean autoFetchLatestOnStartup = false;
    @Builder.Default private final boolean fullFetch = false;

    @Builder.Default private final boolean autoDownloadAudio = false;
    @Builder.Default private final boolean autoDownloadVideo = false;

    @Builder.Default private final boolean addContentDescriptionFiles = false;
    @Builder.Default private final boolean addDownloadDateDir = false;
    @Builder.Default private final boolean separateDirPerFormat = false;

    @Builder.Default private final Period initialFetchPeriod = Period.ofDays(14);

    private static ChannelSettingsBuilder getBuilderWithDefaults() {
        return ChannelSettings.builder()
            .defaultAudio(AudioOnlyQuality.MP3)
            .defaultVideo(VideoQuality.STANDARD)
            .autoFetchLatestOnStartup(false)
            .fullFetch(false)
            .autoDownloadAudio(false)
            .autoDownloadVideo(false)
            .addDownloadDateDir(false)
            .separateDirPerFormat(false)
            .initialFetchPeriod(Period.ofDays(14));
    }

    public static ChannelSettings defaultSettings() {
        return getBuilderWithDefaults().build();
    }
}
