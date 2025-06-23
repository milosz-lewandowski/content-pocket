package pl.mewash.contentlaundry.models.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChannelFetchResultsUi {
    private final SubscribedChannel channel;
    private final List<FetchedUploadUiItem> fetchedItems;
}
