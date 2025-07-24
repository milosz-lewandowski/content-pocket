//package pl.mewash.contentlaundry.service;
//
//import pl.mewash.contentlaundry.models.channel.ChannelFetchRepo;
//import pl.mewash.contentlaundry.models.channel.SubscribedChannel;
//import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
//import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchParams;
//import pl.mewash.contentlaundry.models.content.FetchedContent;
//import pl.mewash.contentlaundry.models.content.FetchingResults;
//import pl.mewash.contentlaundry.utils.AlertUtils;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//public class SubscriptionsService {
//
//    private final ChannelFetchRepo repository = ChannelFetchRepo.getInstance();
//    private final SubscriptionsProcessor subscriptionsProcessor = new SubscriptionsProcessor();
//
//    public ChannelFetchParams fetchContents(String channelName, ChannelFetchParams currentFetchStages) {
//        return switch (currentFetchStages.stage()) {
//            case TO_FETCH -> fetchContents(channelName, null, null);
//            case FETCH_OLDER -> {
//                LocalDateTime fetchAfter = currentFetchStages.fetchOlder().calculateDateAfter();
//                yield fetchOlderContents(channelName, fetchAfter);
//            }
//            default -> throw new IllegalStateException("Unexpected value: " + currentFetchStages.stage());
//        };
//    }
//
//    public ChannelFetchParams fetchOlderContents(String channelName, LocalDateTime fetchOlderDate) {
//        Duration increasedTimeout = Duration.ofMinutes(60);
//        return fetchContents(channelName, increasedTimeout, fetchOlderDate);
//    }
//
//    private ChannelFetchParams fetchContents(String channelName, Duration increasedTimeout, LocalDateTime olderDatetime) {
//        SubscribedChannel channel = repository.getChannel(channelName);
//
//        LocalDateTime fetchDateAfter;
//        if (olderDatetime != null) {
//            fetchDateAfter = olderDatetime;
//        } else if (channel.getLastFetched() == null) {
//            LocalDateTime defaultDate = LocalDateTime.now()
//                    .minusDays(channel.getChannelSettings().getInitialFetchPeriod().getDays());
//            LocalDateTime mostRecentContent = channel.getLatestContentDate();
//            if (mostRecentContent.isBefore(defaultDate)) {
//                fetchDateAfter = mostRecentContent.minusDays(1);
//            } else {
//                fetchDateAfter = defaultDate;
//            }
//        } else {
//            fetchDateAfter = channel.getLastFetched().minusDays(1);
//        }
//
//        Duration currentTimeout = Optional.ofNullable(increasedTimeout)
//                .orElse(channel.getChannelSettings().getDefaultTimeout());
//
//        Optional<FetchingResults> resultsOp = subscriptionsProcessor
//                .fetchUploadsAfter(channel, fetchDateAfter, currentTimeout);
//
//        if (resultsOp.isPresent()) {
//            FetchingResults results = resultsOp.get();
//            List<FetchedContent> fetchedContents = results.fetchedContents();
//
//            if (!results.completedBeforeTimeout()) {
//                long estimatedTimeout = results.estimatedTimeout();
//                boolean retry = AlertUtils.getFetchTimeoutAlertAnswer(
//                        channel.getChannelName(), fetchedContents, fetchDateAfter,
//                        currentTimeout.getSeconds(), estimatedTimeout
//                );
//                if (retry) {
//                    fetchContents(channelName, Duration.ofSeconds(estimatedTimeout), fetchDateAfter);
//                }
//            }
//
//            channel.setLastFetched(LocalDateTime.now());
//            channel.appendFetchedContents(fetchedContents);
//            repository.updateChannel(channel);
//
//            FetchedContent oldestContent = channel.getOldestFetchedContent();
//            ChannelFetchingStage fetchButtonStage = ChannelFetchingStage.FETCH_OLDER;
//            ChannelFetchingStage.FetchOlderRange olderButton = fetchButtonStage.getOlderRange(oldestContent.getPublished());
//
//            return new ChannelFetchParams(fetchButtonStage, olderButton);
//        }
//        return new ChannelFetchParams(ChannelFetchingStage.FETCH_ERROR, null);
//    }
//}
