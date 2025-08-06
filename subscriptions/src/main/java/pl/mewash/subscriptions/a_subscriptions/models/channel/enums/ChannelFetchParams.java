package pl.mewash.subscriptions.a_subscriptions.models.channel.enums;

public record ChannelFetchParams(ChannelFetchingStage stage,
                                 ChannelFetchingStage.FetchOlderRange fetchOlder) {
}
