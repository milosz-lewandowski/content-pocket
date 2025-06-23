package pl.mewash.contentlaundry.service;

import pl.mewash.contentlaundry.models.content.FetchedUpload;

import java.util.List;

public record FetchingResults(
        List<FetchedUpload> fetchedUploads,
        boolean completedBeforeTimeout,
        long estimatedTimeout) {
}
