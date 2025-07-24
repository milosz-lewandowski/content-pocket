package pl.mewash.contentlaundry.models.channel;

import lombok.*;
import pl.mewash.contentlaundry.commands.AudioOnlyQuality;
import pl.mewash.contentlaundry.commands.VideoQuality;

import java.time.Period;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelSettings {

    private AudioOnlyQuality defaultAudio;
    private VideoQuality defaultVideo;

    private boolean autoFetchLastestOnStartup;
    private boolean fullFetch;

    private boolean autoDownloadAudio;
    private boolean autoDownloadVideo;

    private boolean addDownloadDateDir;
    private boolean separateDirPerFormat;

//    private boolean hidePreviouslyFetched;
//    private boolean hideOnBothDownloads;
//    private boolean hideOnAnyDownload;

//    private Duration defaultTimeout;

    private Period initialFetchPeriod;

    private static ChannelSettingsBuilder getBuilderWithDefaults(){
        return ChannelSettings.builder()
                .defaultAudio(AudioOnlyQuality.MP3)
                .defaultVideo(VideoQuality.STANDARD)
                .autoFetchLastestOnStartup(false)
                .fullFetch(false)
                .autoDownloadAudio(false)
                .autoDownloadVideo(false)
                .addDownloadDateDir(false)
                .separateDirPerFormat(false)
                .initialFetchPeriod(Period.ofDays(14));
    }

    public static ChannelSettings defaultSettings(){
        return getBuilderWithDefaults().build();
    }

    @Override
    public String toString() {
        return "ChannelSettings{" +
                "defaultAudio=" + defaultAudio +
                ", defaultVideo=" + defaultVideo +
                ", autoFetchLastestOnStartup=" + autoFetchLastestOnStartup +
                ", fullFetch=" + fullFetch +
                ", autoDownloadAudio=" + autoDownloadAudio +
                ", autoDownloadVideo=" + autoDownloadVideo +
                ", addDownloadDateDir=" + addDownloadDateDir +
                ", separateDirPerFormat=" + separateDirPerFormat +
                ", initialFetchPeriod=" + initialFetchPeriod +
                '}';
    }
}
