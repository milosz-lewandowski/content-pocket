package pl.mewash.subscriptions.internal.service;

import javafx.scene.control.Alert;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.storage.GroupingMode;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.api.DownloadService;
import pl.mewash.common.downloads.api.DownloadServiceProvider;
import pl.mewash.subscriptions.internal.persistence.impl.SubscriptionsJsonRepo;
import pl.mewash.subscriptions.internal.domain.model.ChannelSettings;
import pl.mewash.subscriptions.internal.domain.model.FetchedContent;
import pl.mewash.subscriptions.internal.persistence.repo.SubscriptionsRepository;
import pl.mewash.subscriptions.ui.dialogs.Dialogs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ContentService {

    private final DownloadService downloadService;
    private final SubscriptionsRepository repository;

    public ContentService(AppContext appContext) {
        repository = SubscriptionsJsonRepo.getInstance();
        downloadService = DownloadServiceProvider.getDefaultDownloadService(appContext);
    }

    public void downloadFetched(FetchedContent content, DownloadOption downloadOption, String subsBasePath) {

        ChannelSettings channelSettings = repository.getChannel(content.getChannelUrl()).getChannelSettings();

        StorageOptions storage = new StorageOptions(
            channelSettings.isAddContentDescriptionFiles(),
            channelSettings.isSeparateDirPerFormat() ? GroupingMode.GROUP_BY_FORMAT : GroupingMode.NO_GROUPING,
            channelSettings.isAddDownloadDateDir(),
            false, false
        );

        try {
            Path channelBasePath = Paths.get(subsBasePath + File.separator + content.getChannelName());
            if (!Files.exists(channelBasePath)) Files.createDirectories(channelBasePath);

            Path savedPath = downloadService
                    .downloadWithSettings(content.getUrl(), downloadOption, channelBasePath.toString(), storage);

            content.addAndSetDownloaded(downloadOption, savedPath);
            repository.updateContent(content);

        } catch (Exception e) {
            AppContext.getInstance().getFileLogger()
                    .logErrWithMessage("Downloading error of: " + content.getTitle(), e, true);
            content.setDownloadingErrorState(downloadOption);
            repository.updateContent(content);
            Dialogs.showAlertAndAwait("Download error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
