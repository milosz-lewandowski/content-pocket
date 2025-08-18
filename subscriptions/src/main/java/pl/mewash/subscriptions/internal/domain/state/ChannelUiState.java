package pl.mewash.subscriptions.internal.domain.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pl.mewash.subscriptions.internal.domain.dto.ChannelFetchParams;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;

import java.time.LocalDateTime;


@Getter
@Builder
@AllArgsConstructor
public class ChannelUiState {
    private String channelName;
    private String url;
    private LocalDateTime lastFetched;
    private SubscribedChannel channel;
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
            .channel(channel)
            .lastFetched(channel.getLastFetched())
            .channelSettings(channel.getChannelSettings())
            .fetchingStage(loadingToUiStage)
            .build();
    }

    @JsonIgnore
    public String getFetchButtonTitle() {
        return fetchingStage.getButtonTitleWithOlderResolved(fetchOlderStage);
    }

    public ChannelFetchParams copyCurrentFetchParams() {
        return new ChannelFetchParams(fetchingStage, fetchOlderStage);
    }
}
