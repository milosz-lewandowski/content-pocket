package pl.mewash.contentlaundry.models.channel;

import lombok.*;
import pl.mewash.contentlaundry.models.content.FetchedContentState;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscribedChannel {
    private String channelName;
    private String url;
    private LocalDateTime lastFetched;
    private ChannelSettings channelSettings;
    private Map<String, FetchedContentState> fetchedContentsState;

    public static SubscribedChannel withBasicProperties(String channelName, String url) {
        return SubscribedChannel.builder()
                .channelName(channelName)
                .url(url)
                .build();
    }
}
