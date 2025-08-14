package pl.mewash.subscriptions.a_subscriptions.services;

import javafx.scene.control.Alert;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.storage.GroupingMode;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.api.DownloadService;
import pl.mewash.common.downloads.api.DownloadServiceProvider;
import pl.mewash.subscriptions.a_subscriptions.AlertUtils;
import pl.mewash.subscriptions.a_subscriptions.models.channel.ChannelFetchRepo;
import pl.mewash.subscriptions.a_subscriptions.models.channel.ChannelSettings;
import pl.mewash.subscriptions.a_subscriptions.models.content.FetchedContent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ContentService {

    private final DownloadService downloadService;

    public ContentService(AppContext appContext) {
        downloadService = DownloadServiceProvider.getDefaultDownloadService(appContext);
    }

    public void downloadFetched(FetchedContent content, DownloadOption downloadOption, String subsBasePath) {
        ChannelFetchRepo repository = ChannelFetchRepo.getInstance();

        ChannelSettings channelSettings = repository.getChannelSettings(content.getChannelName());

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
            e.printStackTrace();
            content.setDownloadingError(downloadOption);
            repository.updateContent(content);
            AlertUtils.showAlertAndAwait("Download error", e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
