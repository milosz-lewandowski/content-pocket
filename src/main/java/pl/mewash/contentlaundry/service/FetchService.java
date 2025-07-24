package pl.mewash.contentlaundry.service;

import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchParams;
import pl.mewash.contentlaundry.models.content.FetchedContent;
import pl.mewash.contentlaundry.models.content.FetchingResults;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class FetchService {

    private final ChannelFetchRepo repository = ChannelFetchRepo.getInstance();
    private final SubscriptionsProcessor subscriptionsProcessor = new SubscriptionsProcessor();

    public ChannelFetchParams fetch(String channelName, ChannelFetchParams fetchParams) {
        SubscribedChannel channel = repository.getChannel(channelName);

        LocalDateTime dateAfter = calculateDateAfter(channel, fetchParams.stage(), fetchParams.fetchOlder());
        Duration timeout = calculateTimeout();

        Optional<FetchingResults> resultsOp = subscriptionsProcessor.fetchUploadsAfter(channel, dateAfter, timeout);

//        System.out.println(" after fetch in Fetch Service");
        if (resultsOp.isPresent()) {
            FetchingResults results = resultsOp.get();
            List<FetchedContent> fetchedContents = results.fetchedContents();
//            System.out.println(" results present in Fetch Service");

            // if required insert timeout logic

            channel.setLastFetched(LocalDateTime.now());
            channel.appendFetchedContents(fetchedContents);
            channel.setPreviousFetchOlderRangeDate(dateAfter);
            repository.updateChannel(channel);
//            System.out.println("after channel update in Fetch Service");

            SubscribedChannel updatedChannel = repository.getChannel(channelName); // test if re-synchro helps
//            System.out.println(" updatedChannel present in Fetch Service");

            LocalDateTime oldestContentOrPreviousFetchDateAfter = updatedChannel.calculateNextFetchOlderInputDate();
            ChannelFetchingStage fetchButtonStage = ChannelFetchingStage.FETCH_OLDER;
            ChannelFetchingStage.FetchOlderRange fetchOlderRange = fetchButtonStage
                    .getOlderRange(oldestContentOrPreviousFetchDateAfter);
//            System.out.println(" fetchOlderRange present in Fetch Service: " + fetchOlderRange.getButtonTitle());

            return new ChannelFetchParams(ChannelFetchingStage.FETCH_OLDER, fetchOlderRange);

            // if fetch failed return error state
        } else return new ChannelFetchParams(ChannelFetchingStage.FETCH_ERROR, null);
    }

    // FIXME: insert logic
    private Duration calculateTimeout(){
        return Duration.ofSeconds(180);
    }

    private LocalDateTime calculateDateAfter(SubscribedChannel channel,
                                             ChannelFetchingStage stage,
                                             ChannelFetchingStage.FetchOlderRange fetchOlderRange
    ) {
        return switch (stage) {
            case FIRST_FETCH -> {
                LocalDateTime defaultChannelDateAfter = LocalDateTime.now()
                        .minus(channel.getChannelSettings().getInitialFetchPeriod());
                LocalDateTime latestChannelContentDate = channel.getLatestContentOnChannelDate();

                yield latestChannelContentDate.isBefore(defaultChannelDateAfter)
                        ? latestChannelContentDate.minusDays(1)
                        : defaultChannelDateAfter;
            }
            case FETCH_LATEST -> {
                LocalDateTime lastFetchedDate = channel.getLastFetched();
                LocalDateTime mostRecentFetchedContent = channel.findMostRecentFetchedContent().getPublished();

                yield mostRecentFetchedContent.isBefore(lastFetchedDate)
                        ? mostRecentFetchedContent.minusDays(1)
                        : lastFetchedDate.minusDays(1);
            }
            case FETCH_OLDER -> fetchOlderRange.calculateDateAfter();

            default -> throw new IllegalStateException("Channel in " + stage + " stage cannot be fetched " + channel.getChannelName());
        };
    }
}
