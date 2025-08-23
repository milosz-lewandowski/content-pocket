package pl.mewash.common.downloads.api;

import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.io.IOException;
import java.nio.file.Path;

public interface DownloadService {

    DownloadResults downloadWithSettings(String contentUrl, DownloadOption downloadSettings, String baseDirPath,
                                                                StorageOptions storageOptions) throws IOException, InterruptedException;

    record DownloadResults(String title, Path downloadedPath){}
}
