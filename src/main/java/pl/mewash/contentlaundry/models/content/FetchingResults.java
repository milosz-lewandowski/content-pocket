package pl.mewash.contentlaundry.models.content;

import java.util.List;

public record FetchingResults(
        List<FetchedContent> fetchedContents,
        boolean completedBeforeTimeout,
        long estimatedTimeout) {
}
