package pl.mewash.subscriptions.internal.domain.dto;

import pl.mewash.subscriptions.internal.domain.state.ChannelFetchingStage;

public record ChannelFetchParams(ChannelFetchingStage stage,
                                 ChannelFetchingStage.FetchOlderRange fetchOlder) {
}
