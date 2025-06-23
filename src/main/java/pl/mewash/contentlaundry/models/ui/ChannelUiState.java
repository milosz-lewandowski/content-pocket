package pl.mewash.contentlaundry.models.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;

@AllArgsConstructor
@Getter
public class ChannelUiState {
    private final SubscribedChannel subscribedChannel;
    @Setter
    private ChannelFetchingStage fetchingStage;
}
