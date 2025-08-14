package pl.mewash.subscriptions.internal.persistence.repo;

import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;

import java.util.List;
import java.util.Optional;

public interface SubscriptionsRepository {

    // --- channel UI state representation ---
    List<ChannelUiState> loadChannelsUiList();
    ChannelUiState getChannelUiState(String channelUrl);

    // --- channels ---
    Optional<SubscribedChannel> findChannelIfUrlExists(String channelUrl);
    SubscribedChannel getChannel(String channelUrl);
    void addChannel(SubscribedChannel channel);
    void updateChannel(SubscribedChannel subscribedChannel);

    // --- channel settings ---
    void updateChannelSettingsFromState(ChannelUiState channelUiState);
    ChannelSettings getChannelSettings(String channelUrl);

    // --- contents ---
    void updateContent(FetchedContent fetchedContent);
    List<FetchedContent> getAllChannelContents(String channelUrl);
}
