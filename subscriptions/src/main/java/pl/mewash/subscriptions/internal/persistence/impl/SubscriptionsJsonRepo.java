package pl.mewash.subscriptions.internal.persistence.impl;

import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.domain.state.ChannelUiState;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SubscriptionsJsonRepo implements SubscriptionsRepository {

    private static final SubscriptionsJsonRepo INSTANCE = new SubscriptionsJsonRepo();

    public static SubscriptionsJsonRepo getInstance() {
        return INSTANCE;
    }

    private SubscriptionsJsonRepo() {
    }

    private final ConcurrentHashMap<String, SubscribedChannel> channelsUrlsMap = new ConcurrentHashMap<>();

    public void load() {
        try {
            List<SubscribedChannel> loaded = SubscriptionsJsonManager.loadChannels();
            Map<String, SubscribedChannel> channelsMap = loaded.stream()
                .collect(Collectors.toMap(
                    SubscribedChannel::getUrl,
                    Function.identity(),
                    (existing, newValue) -> existing,
                    ConcurrentHashMap::new
                ));
            channelsUrlsMap.clear();
            channelsUrlsMap.putAll(channelsMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void persist() {
        SubscriptionsJsonManager.saveChannels(channelsUrlsMap.values().stream().toList());
    }

    public List<ChannelUiState> loadChannelsUiList() {
        return channelsUrlsMap.values().stream()
            .map(ChannelUiState::fromChannelInit)
            .toList();
    }

    public ChannelUiState getChannelUiState(String channelUrl) {
        return ChannelUiState.fromChannelInit(channelsUrlsMap.get(channelUrl));
    }

    public SubscribedChannel getChannel(String channelUrl) {
        return channelsUrlsMap.get(channelUrl);
    }

    public Optional<SubscribedChannel> findChannelIfUrlExists(String channelUrl) {
        return channelsUrlsMap.values().stream()
            .filter(subscribedChannel -> subscribedChannel.checkUrlMatch(channelUrl))
            .findAny();
    }

    public void addChannel(SubscribedChannel channel) {
        channelsUrlsMap.put(channel.getUrl(), channel);
        persist();
    }

    public ChannelSettings getChannelSettings(String channelUrl) {
        return channelsUrlsMap.get(channelUrl).getChannelSettings();
    }

    public void updateChannel(SubscribedChannel subscribedChannel) {
        channelsUrlsMap.put(subscribedChannel.getUrl(), subscribedChannel);
        persist();
    }

    public void updateChannelSettingsFromState(ChannelUiState channelUiState) {
        SubscribedChannel subscribedChannel = channelsUrlsMap.get(channelUiState.getUrl());
        subscribedChannel.setChannelSettings(channelUiState.getChannelSettings());
        channelsUrlsMap.put(subscribedChannel.getUrl(), subscribedChannel);
        persist();
    }

    public void updateContent(FetchedContent fetchedContent) {
        SubscribedChannel subscribedChannel = getChannel(fetchedContent.getChannelUrl());
        subscribedChannel.updateFetchedContent(fetchedContent);
        updateChannel(subscribedChannel);
    }

    public List<FetchedContent> getAllChannelContents(String channelUrl) {
        SubscribedChannel subscribedChannel = channelsUrlsMap.get(channelUrl);
        boolean exceedsYear = subscribedChannel.shouldAddYear();
        return subscribedChannel.getFetchedContentMap().values().stream()
            .map(content -> content.updateDisplayTitle(exceedsYear))
            .sorted(Comparator.comparing(FetchedContent::getPublished).reversed())
            .toList();
    }
}
