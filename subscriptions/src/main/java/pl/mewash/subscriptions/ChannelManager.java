package pl.mewash.subscriptions;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Alert;
import pl.mewash.common.app.config.ConfigPaths;
import pl.mewash.common.app.config.JsonMapperConfig;
import pl.mewash.subscriptions.a_subscriptions.AlertUtils;
import pl.mewash.subscriptions.a_subscriptions.models.channel.SubscribedChannel;


import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ChannelManager {
    private static final ObjectMapper mapper = JsonMapperConfig.getPrettyMapper();

    public static List<SubscribedChannel> loadChannels() throws IOException {
        ConfigPaths.ensureConfigDirExists();
        if (!Files.exists(ConfigPaths.SUBSCRIPTIONS_FILE)) return new ArrayList<>();

        return mapper.readValue(ConfigPaths.SUBSCRIPTIONS_FILE.toFile(),
                new TypeReference<>() {});
    }

    public static void saveChannels(List<SubscribedChannel> channels){
        try {
            ConfigPaths.ensureConfigDirExists();
            mapper.writeValue(ConfigPaths.SUBSCRIPTIONS_FILE.toFile(), channels);
        } catch (IOException e) {
            AlertUtils.showAlertAndAwait("Subscription saving error",
                    "Error encountered while saving subscribed channels",
                    Alert.AlertType.ERROR);
            e.printStackTrace();
        }

    }
}