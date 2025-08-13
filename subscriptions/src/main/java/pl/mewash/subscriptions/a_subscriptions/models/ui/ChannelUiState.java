package pl.mewash.subscriptions.a_subscriptions.models.ui;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pl.mewash.subscriptions.a_subscriptions.models.channel.ChannelSettings;
import pl.mewash.subscriptions.a_subscriptions.models.channel.SubscribedChannel;
import pl.mewash.subscriptions.a_subscriptions.models.channel.enums.ChannelFetchingStage;
import pl.mewash.subscriptions.a_subscriptions.models.channel.enums.ChannelFetchParams;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
public class ChannelUiState {
    private String channelName;
    private String url;
    private LocalDateTime lastFetched;
    @Setter
    private ChannelSettings channelSettings;
    @Setter
    private ChannelFetchingStage fetchingStage;
    @Setter
    private ChannelFetchingStage.FetchOlderRange fetchOlderStage;

    public static ChannelUiState fromChannelInit(SubscribedChannel channel) {

        ChannelFetchingStage loadingToUiStage = channel.getLastFetched() == null
                ? ChannelFetchingStage.FIRST_FETCH
                : ChannelFetchingStage.FETCH_LATEST;

        return ChannelUiState.builder()
                .channelName(channel.getChannelName())
                .url(channel.getUrl())
                .lastFetched(channel.getLastFetched())
                .channelSettings(channel.getChannelSettings())
                .fetchingStage(loadingToUiStage)
                .build();
    }

    @JsonIgnore
    public String getFetchButtonTitle() {
        if (channelSettings.isFullFetch()) {
            setFetchingStage(ChannelFetchingStage.FETCH_OLDER);
            setFetchOlderStage(ChannelFetchingStage.FetchOlderRange.LAST_25_YEARS);
        }
        return fetchingStage.getButtonTitleWithOlderResolved(fetchOlderStage);
    }

    public ChannelFetchParams copyCurrentFetchParams() {
        return new ChannelFetchParams(fetchingStage, fetchOlderStage);
    }
}
