package pl.mewash.contentlaundry.models.channel;

import lombok.*;
import pl.mewash.contentlaundry.models.general.enums.Formats;

import java.time.Duration;
import java.util.EnumSet;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSettings {

    private EnumSet<Formats> defaultFormats;

    private boolean autoFetch;
    private boolean autoDownload;
    private boolean trackHistory;

    private boolean skipPreviouslyFetchedPrint;

    private boolean skipAnyDownloadedPrint;
    private boolean allowReDownload;

    private Duration timeout;

    private static ChannelSettingsBuilder getBuilderWithDefaults(){
        return ChannelSettings.builder()
                .autoFetch(false)
                .autoDownload(false)
                .trackHistory(true)
                .skipPreviouslyFetchedPrint(false)
                .skipAnyDownloadedPrint(false)
                .allowReDownload(false)
                .timeout(Duration.ofSeconds(30));
    }

    public static ChannelSettings defaultVideoSettings() {
        return getBuilderWithDefaults()
                .defaultFormats(EnumSet.of(Formats.MP4))
                .build();
    }

    public static ChannelSettings defaultAudioSettings() {
        return getBuilderWithDefaults()
                .defaultFormats(EnumSet.of(Formats.MP3))
                .build();
    }

//    public static ChannelSettings defaultNewsChannelVideoSettings() {
//        return null;
//    }
//
//    public static ChannelSettings defaultPodcastAudioSettings() {
//        return null;
//    }
}
