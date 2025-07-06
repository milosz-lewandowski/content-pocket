package pl.mewash.contentlaundry.models.content;

import java.util.List;

public record FetchingResults(
        List<FetchedUpload> fetchedUploads,
        boolean completedBeforeTimeout,
        long estimatedTimeout) {
}
