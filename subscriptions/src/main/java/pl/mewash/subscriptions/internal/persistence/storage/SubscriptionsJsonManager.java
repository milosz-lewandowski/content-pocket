package pl.mewash.subscriptions.internal.persistence.storage;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.common.app.config.ConfigPaths;
import pl.mewash.common.app.config.JsonMapperConfig;
import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.common.logging.api.LoggersProvider;
import pl.mewash.subscriptions.internal.domain.model.SubscribedChannel;
import pl.mewash.subscriptions.internal.persistence.config.DownloadOptionMixin;
import pl.mewash.subscriptions.ui.dialogs.Dialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionsJsonManager {
    private static final ObjectMapper subscriptionsMapper = configureSubsMapper();
    private static Path cachedSubscriptionsPath;

    private static ObjectMapper configureSubsMapper(){
        ObjectMapper mapper = JsonMapperConfig.getPrettyMapper().copy();
        mapper.addMixIn(DownloadOption.class, DownloadOptionMixin.class);
        return mapper;
    }

    public static List<SubscribedChannel> loadChannels() throws IOException {
        Path subscriptionsFilePath = resolveSubscriptionsFilePath();

        if (!Files.exists(subscriptionsFilePath)) return new ArrayList<>();

        return subscriptionsMapper.readValue(subscriptionsFilePath.toFile(),
                new TypeReference<>() {});
    }

    public static void saveChannels(List<SubscribedChannel> channels){
        try {
            Path subscriptionsFilePath = resolveSubscriptionsFilePath();
            subscriptionsMapper.writeValue(subscriptionsFilePath.toFile(), channels);
        } catch (IOException e) {
            Dialogs.showAlertAndAwait("Subscription saving error",
                    "Error encountered while saving subscribed channels",
                    Alert.AlertType.ERROR);
            LoggersProvider.getFileLogger().logErrStackTrace(e, true);
        }

    }

    private static Path resolveSubscriptionsFilePath() throws IOException {
        if (cachedSubscriptionsPath != null) return cachedSubscriptionsPath;

        GeneralSettings settings = SettingsManager.loadSettings();
        String savedSubsPathString = settings.getSavedSubscriptionsFilePath();

        if (savedSubsPathString != null) {
            Path savedSubsFilePath = Paths.get(savedSubsPathString);
            if (Files.exists(savedSubsFilePath)) {
                cachedSubscriptionsPath = savedSubsFilePath;
                return cachedSubscriptionsPath;
            }
        }
        cachedSubscriptionsPath = ConfigPaths.getSubscriptionsFilePath();
        LoggersProvider.getFileLogger()
            .appendSingleLine("Loading subscribed channels from:\n" + cachedSubscriptionsPath);

        settings.setSavedSubscriptionsFilePath(cachedSubscriptionsPath.toAbsolutePath().toString());
        SettingsManager.saveSettings(settings);

        return cachedSubscriptionsPath;
    }
}