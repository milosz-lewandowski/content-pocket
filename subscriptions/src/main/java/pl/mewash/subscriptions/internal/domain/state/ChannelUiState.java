package pl.mewash.subscriptions.internal.domain.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;


@Getter
@Builder
public class ChannelUiState {
    private String channelName;
    private final String url;
    private final SubscribedChannel channel;

    private ProgressiveFetchRange fetchRange;
    private final ObjectProperty<ProgressiveFetchStage>
        fetchingStage = new SimpleObjectProperty<>(ProgressiveFetchStage.FETCH_LATEST);


    public static ChannelUiState fromChannelInit(SubscribedChannel channel) {

        ChannelUiState channelUiState = ChannelUiState.builder()
            .channelName(channel.getChannelName())
            .url(channel.getUniqueUrl())
            .channel(channel)
            .build();

        ProgressiveFetchStage initialStage = channel.getLastFetched() == null
            ? ProgressiveFetchStage.FIRST_FETCH
            : ProgressiveFetchStage.FETCH_LATEST;
        channelUiState.setFetchingStage(initialStage);

        return channelUiState;
    }

    @JsonIgnore
    public void setFetchingStage(ProgressiveFetchStage fetchStage) {
        updateFetchingStageProperty(fetchStage);
    }

    @JsonIgnore
    public void setFetchingStageWithRange(ProgressiveFetchRange range, ProgressiveFetchStage fetchStage) {
        this.fetchRange = range;
        updateFetchingStageProperty(fetchStage);
    }

    @JsonIgnore
    public String getFetchButtonTitle() {
        return fetchingStage.get().getButtonTitleWithOlderResolved(fetchRange);
    }

    @JsonIgnore
    public boolean getButtonDisabled() {
        return fetchingStage.get().isDisabled();
    }

    @JsonIgnore
    public boolean isBeforeFirstFetch() {
        return this.getChannel().getPreviousFetchOlderRangeDate() == null;
    }

    @JsonIgnore
    private void updateFetchingStageProperty(ProgressiveFetchStage fetchingStage) {
        if (Platform.isFxApplicationThread()) this.fetchingStage.set(fetchingStage);
        else Platform.runLater(() -> this.fetchingStage.set(fetchingStage));
    }
}
