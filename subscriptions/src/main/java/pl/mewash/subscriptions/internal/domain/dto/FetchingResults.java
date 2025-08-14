package pl.mewash.subscriptions.internal.domain.dto;

import pl.mewash.subscriptions.internal.domain.model.FetchedContent;

import java.util.List;

public record FetchingResults(
        List<FetchedContent> fetchedContents,
        boolean completedBeforeTimeout,
        long estimatedTimeout) {
}
