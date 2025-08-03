package pl.mewash.contentlaundry.models.channel;

import pl.mewash.contentlaundry.models.content.FetchedContent;
import pl.mewash.contentlaundry.models.ui.ChannelUiState;
import pl.mewash.contentlaundry.subscriptions.ChannelManager;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChannelFetchRepo {

    private static final ChannelFetchRepo INSTANCE = new ChannelFetchRepo();
    public static ChannelFetchRepo getInstance() {return INSTANCE;}
    private ChannelFetchRepo() {}

    private final ConcurrentHashMap<String, SubscribedChannel> channelsNamesMap = new ConcurrentHashMap<>();

    public void load(){
        try {
            List<SubscribedChannel> loaded = ChannelManager.loadChannels();
            Map<String, SubscribedChannel> channelsMap = loaded.stream()
                    .collect(Collectors.toMap(
                            SubscribedChannel::getChannelName,
                            Function.identity(),
                            (existing, newValue) -> existing,
                            ConcurrentHashMap::new
                    ));
            channelsNamesMap.clear();
            channelsNamesMap.putAll(channelsMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void persist(){
        ChannelManager.saveChannels(channelsNamesMap.values().stream().toList());
    }

    public List<ChannelUiState> loadChannelsUiList(){
        return channelsNamesMap.values().stream()
                .map(ChannelUiState::fromChannelInit)
                .toList();
    }

    public ChannelUiState getChannelUiState(String channelName){
        return ChannelUiState.fromChannelInit(channelsNamesMap.get(channelName));
    }

    public SubscribedChannel getChannel(String channelName) {
        return channelsNamesMap.get(channelName);
    }

    public Optional<SubscribedChannel> checkChannelExists(String channelUrl) {
        return channelsNamesMap.values().stream()
                .filter(subscribedChannel -> subscribedChannel.getUrl().equals(channelUrl))
                .findAny();

    }

    public void addChannel(SubscribedChannel channel) {
        channelsNamesMap.put(channel.getChannelName(), channel);
        persist();
    }

    public ChannelSettings getChannelSettings(String channelName) {
        return channelsNamesMap.get(channelName).getChannelSettings();
    }

    public void updateChannel(SubscribedChannel subscribedChannel) {
        channelsNamesMap.put(subscribedChannel.getChannelName(), subscribedChannel);
        persist();
    }

    public void updateChannelSettingsFromState(ChannelUiState channelUiState) {
        SubscribedChannel subscribedChannel = channelsNamesMap.get(channelUiState.getChannelName());
        subscribedChannel.setChannelSettings(channelUiState.getChannelSettings());
        channelsNamesMap.put(subscribedChannel.getChannelName(), subscribedChannel);
        persist();
    }

    public void updateContent(FetchedContent fetchedContent) {
        SubscribedChannel subscribedChannel = getChannel(fetchedContent.getChannelName());
        subscribedChannel.updateFetchedContent(fetchedContent);
        updateChannel(subscribedChannel);
    }

    public List<FetchedContent> getAllChannelContents(String channelName) {
        SubscribedChannel subscribedChannel = channelsNamesMap.get(channelName);
        boolean exceedsYear = subscribedChannel.shouldAddYear();
        return subscribedChannel.getFetchedContentMap().values().stream()
                .map(content -> content.updateDisplayTitle(exceedsYear))
                .sorted(Comparator.comparing(FetchedContent::getPublished).reversed())
                .toList();
    }
}
