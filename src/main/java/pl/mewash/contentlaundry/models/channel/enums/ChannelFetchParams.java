package pl.mewash.contentlaundry.models.channel.enums;

public record ChannelFetchParams(ChannelFetchingStage stage,
                                 ChannelFetchingStage.FetchOlderRange fetchOlder) {
}
