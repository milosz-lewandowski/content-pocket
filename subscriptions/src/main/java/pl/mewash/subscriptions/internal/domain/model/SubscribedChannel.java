package pl.mewash.subscriptions.internal.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscribedChannel {
    private String channelName;
    private String url; // globally unique id
    private String officialUrl;
    private String initialUrl;
    private LocalDateTime lastFetched;
    private ChannelSettings channelSettings;

    // fetch params
    private LocalDateTime latestContentOnChannelDate;
    private LocalDateTime previousFetchOlderRangeDate;
    private boolean fetchedSinceOldest;

    @Builder.Default
    private ConcurrentHashMap<String, FetchedContent> fetchedContentMap = new ConcurrentHashMap<>();

    public static SubscribedChannel withBasicProperties(String channelName, String url) {
        return SubscribedChannel.builder()
            .channelName(channelName)
            .url(url)
            .build();
    }

    public boolean checkUrlMatch(String channelUrl) {
        BiPredicate<String, String> fieldNotNullAndEqual = (fieldValue, url) -> fieldValue != null
            && fieldValue.equals(url);

        return fieldNotNullAndEqual.test(url, channelUrl)
            || fieldNotNullAndEqual.test(officialUrl, channelUrl)
            || fieldNotNullAndEqual.test(initialUrl, channelUrl);
    }

    public static SubscribedChannel withLatestContent(String initialUrl, String channelName, String uniqueUrl,
                                                      LocalDate latestPublished) {

        LocalDateTime publishedDateTime = LocalDateTime.of(latestPublished, LocalTime.MIN);
        String officialUrl = initialUrl.contains("@")
            ? initialUrl
            : null;
        return SubscribedChannel.builder()
            .channelName(channelName)
            .url(uniqueUrl)
            .initialUrl(initialUrl)
            .officialUrl(officialUrl)
            .latestContentOnChannelDate(publishedDateTime)
            .build();
    }

    public boolean appendFetchedContentsIfNotPresent(List<FetchedContent> fetchedContents) {
        Map<String, FetchedContent> newOnes = fetchedContents.stream()
            .filter(fc -> !fetchedContentMap.containsKey(fc.getId()))
            .collect(Collectors.toMap(FetchedContent::getId, fc -> fc));

        fetchedContentMap.putAll(newOnes);
        return !newOnes.isEmpty();
    }

    public void updateFetchedContent(FetchedContent fetchedContent) {
        fetchedContentMap.put(fetchedContent.getId(), fetchedContent);
    }


    public LocalDateTime calculateNextFetchOlderInputDate() {
        LocalDateTime oldestContent = findOldestFetchedContent().getPublished();
        LocalDateTime previousFetchRange = getPreviousFetchOlderRangeDate();
        return oldestContent.isBefore(previousFetchRange)
            ? oldestContent
            : previousFetchRange;
    }

    @JsonIgnore
    public FetchedContent findMostRecentFetchedContent() {
        return fetchedContentMap.values().stream()
            .filter(content -> content.getPublished() != null)
            .max(Comparator.comparing(FetchedContent::getPublished))
            .orElse(null);
    }

    @JsonIgnore
    public FetchedContent findOldestFetchedContent() {
        return fetchedContentMap.values().stream()
            .filter(content -> content.getPublished() != null)
            .min(Comparator.comparing(FetchedContent::getPublished))
            .orElse(null);
    }

    public boolean shouldAddYear() {
        return fetchedContentMap.values().stream()
            .map(content -> content.getPublished().getYear())
            .anyMatch(year -> !year.equals(LocalDateTime.now().getYear()));
    }
}
