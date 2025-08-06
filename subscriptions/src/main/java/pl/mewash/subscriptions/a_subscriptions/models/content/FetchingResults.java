package pl.mewash.subscriptions.a_subscriptions.models.content;

import java.util.List;

public record FetchingResults(
        List<FetchedContent> fetchedContents,
        boolean completedBeforeTimeout,
        long estimatedTimeout) {
}
