package pl.mewash.subscriptions.internal.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

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
@Builder
@Jacksonized
public class SubscribedChannel {
    private String channelName;
    private final String uniqueUrl;
    private final String officialUrl;
    private final String initialUrl;

    @Setter private LocalDateTime lastFetched;
    @Setter private ChannelSettings channelSettings;

    @Setter private LocalDateTime latestContentOnChannelDate;
    @Setter private LocalDateTime previousFetchOlderRangeDate;
    @Setter private boolean fetchedSinceOldest;

    @Builder.Default
    private ConcurrentHashMap<String, FetchedContent> fetchedContentMap = new ConcurrentHashMap<>();

    public static SubscribedChannel withLatestContent(String initialUrl, String channelName, String uniqueUrl,
                                                      LocalDate latestPublished) {
        LocalDateTime publishedDateTime = LocalDateTime.of(latestPublished, LocalTime.MIN);
        String officialUrl = initialUrl.contains("@")
            ? initialUrl
            : null;

        return SubscribedChannel.builder()
            .channelName(channelName)
            .uniqueUrl(uniqueUrl)
            .initialUrl(initialUrl)
            .officialUrl(officialUrl)
            .latestContentOnChannelDate(publishedDateTime)
            .build();
    }

    public boolean checkUrlMatch(String channelUrl) {
        BiPredicate<String, String> fieldNotNullAndEqual = (fieldValue, url) -> fieldValue != null
            && fieldValue.equals(url);

        return fieldNotNullAndEqual.test(uniqueUrl, channelUrl)
            || fieldNotNullAndEqual.test(officialUrl, channelUrl)
            || fieldNotNullAndEqual.test(initialUrl, channelUrl);
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
