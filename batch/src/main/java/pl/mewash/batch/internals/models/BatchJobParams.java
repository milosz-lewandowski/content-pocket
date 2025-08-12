package pl.mewash.batch.internals.models;

import lombok.Builder;
import lombok.Getter;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
public class BatchJobParams {
    @Getter private final String basePath;
    @Getter private final Set<DownloadOption> selectedDownloadOptions;
    @Getter private final List<String> urlList;
    @Getter private final StorageOptions storageOptions;
    @Getter private final MultithreadingMode multithreadingMode;

    @Getter @Builder.Default private final AtomicInteger completedCount = new AtomicInteger(0);
    @Getter @Builder.Default private final AtomicInteger failedCount = new AtomicInteger(0);

    private int totalDownloads;

    public int calculateTotalDownloads() {
        if (totalDownloads != 0) return totalDownloads;
        totalDownloads = urlList.size() * selectedDownloadOptions.size();
        return totalDownloads;
    }

    public boolean checkAllDownloadsCompleted() {
        return completedCount.get() + failedCount.get() == calculateTotalDownloads();
    }
}
