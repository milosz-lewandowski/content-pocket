package pl.mewash.subscriptions.a_subscriptions.models.channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import pl.mewash.subscriptions.a_subscriptions.models.content.FetchedContent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscribedChannel {
    private String channelName;
    private String url;
    private LocalDateTime lastFetched;
    private ChannelSettings channelSettings;

    // fetch params
    private LocalDateTime latestContentOnChannelDate;
    private LocalDateTime previousFetchOlderRangeDate;

    @Builder.Default
    private ConcurrentHashMap<String, FetchedContent> fetchedContentMap = new ConcurrentHashMap<>();

    public static SubscribedChannel withBasicProperties(String channelName, String url) {
        return SubscribedChannel.builder()
                .channelName(channelName)
                .url(url)
                .build();
    }

    public static SubscribedChannel withLatestContent(String channelName, String channelUrl, LocalDate latestPublished) {

        LocalDateTime publishedDateTime = LocalDateTime.of(latestPublished, LocalTime.MIN);

        return SubscribedChannel.builder()
                .channelName(channelName)
                .url(channelUrl)
                .latestContentOnChannelDate(publishedDateTime)
                .build();
    }

    public void appendFetchedContents(List<FetchedContent> fetchedContents) {
        fetchedContents.stream()
                .filter(fetchedContent -> !fetchedContentMap.containsKey(fetchedContent.getId()))
                .forEach(fetchedContent -> fetchedContentMap.put(fetchedContent.getId(), fetchedContent));
    }

    public void updateFetchedContent(FetchedContent fetchedContent) {
        fetchedContentMap.put(fetchedContent.getId(), fetchedContent);
    }


    public LocalDateTime calculateNextFetchOlderInputDate(){
        LocalDateTime oldestContent = findOldestFetchedContent().getPublished();
        LocalDateTime previousFetchRange = getPreviousFetchOlderRangeDate();
        return oldestContent.isBefore(previousFetchRange)
                ? oldestContent
                : previousFetchRange;
    }

    @JsonIgnore
    public FetchedContent findMostRecentFetchedContent(){
        return fetchedContentMap.values().stream()
                .filter(content -> content.getPublished() != null)
                .max(Comparator.comparing(FetchedContent::getPublished))
                .orElse(null);
    }

    @JsonIgnore
    public FetchedContent findOldestFetchedContent(){
        return fetchedContentMap.values().stream()
                .filter(content -> content.getPublished() != null)
                .min(Comparator.comparing(FetchedContent::getPublished))
                .orElse(null);
    }

    public boolean shouldAddYear(){
        return fetchedContentMap.values().stream()
                .map(content -> content.getPublished().getYear())
                .anyMatch(year -> !year.equals(LocalDateTime.now().getYear()));
    }
}
