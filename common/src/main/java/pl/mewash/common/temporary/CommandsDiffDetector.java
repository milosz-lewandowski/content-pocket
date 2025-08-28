package pl.mewash.common.temporary;

import lombok.Getter;
import pl.mewash.commands.api.processes.ProcessFactory;
import pl.mewash.commands.api.processes.ProcessFactoryProvider;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.common.app.context.AppContext;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class CommandsDiffDetector implements ProcessFactory {



    @Override
    public ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile) {
        FactoryComparator factoryComparator = new FactoryComparator();
        factoryComparator.getLocalDefault().fetchChannelBasicData(channelUrl, channelProperties, tempFile);
        factoryComparator.getLocalLegacy().fetchChannelBasicData(channelUrl, channelProperties, tempFile);

        compareCommands(factoryComparator.legacyCmd.get(), factoryComparator.defaultCmd.get());
        return null;
    }

    @Override
    public ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate, ContentProperties contentProperties, Path tempFile) {
        FactoryComparator factoryComparator = new FactoryComparator();
        factoryComparator.getLocalDefault().fetchContentsPublishedAfter(channelUrl, afterDate, contentProperties, tempFile);
        factoryComparator.getLocalLegacy().fetchContentsPublishedAfter(channelUrl, afterDate, contentProperties, tempFile);

        compareCommands(factoryComparator.legacyCmd.get(), factoryComparator.defaultCmd.get());
        return null;
    }

    @Override
    public ProcessBuilder downloadAudioStream(String url, AudioOnlyQuality audioQuality, StorageOptions storageOptions, Path tempFile) {
        FactoryComparator factoryComparator = new FactoryComparator();
        factoryComparator.getLocalDefault().downloadAudioStream(url, audioQuality, storageOptions, tempFile);
        factoryComparator.getLocalLegacy().downloadAudioStream(url, audioQuality, storageOptions, tempFile);

        compareCommands(factoryComparator.legacyCmd.get(), factoryComparator.defaultCmd.get());
        return null;
    }

    @Override
    public ProcessBuilder downloadVideoWithAudioStream(String url, VideoQuality videoQuality, StorageOptions storageOptions, Path tempFile) {
        FactoryComparator factoryComparator = new FactoryComparator();
        factoryComparator.getLocalDefault().downloadVideoWithAudioStream(url, videoQuality, storageOptions, tempFile);
        factoryComparator.getLocalLegacy().downloadVideoWithAudioStream(url, videoQuality, storageOptions, tempFile);

        compareCommands(factoryComparator.legacyCmd.get(), factoryComparator.defaultCmd.get());
        return null;
    }


    class FactoryComparator {
        AtomicReference<String> defaultCmd = new AtomicReference<>();
        AtomicReference<String> legacyCmd = new AtomicReference<>();
        Consumer<String> defaultConsumer = defaultCmd::set;
        Consumer<String> legacyConsumer = legacyCmd::set;

        @Getter
        ProcessFactory localDefault = ProcessFactoryProvider.createDefaultFactoryWithLogger(
            AppContext.getInstance().getYtDlpCommand(), AppContext.getInstance().getFfMpegCommand(),
            defaultConsumer, false);

        @Getter
        ProcessFactory localLegacy = ProcessFactoryProvider.createLegacyFactoryWithLogger(
            AppContext.getInstance().getYtDlpCommand(), AppContext.getInstance().getFfMpegCommand(),
            legacyConsumer, false);
    }



    void compareCommands(String legacyCmd, String defaultCmd) {

        Set<String> defaultArgs = tokenizeCommand(defaultCmd);
        Set<String> legacyArgs = tokenizeCommand(legacyCmd);

        if (!defaultArgs.equals(legacyArgs)) {
            System.out.println("--- DEFAULT ---");
            System.out.println(defaultCmd);
            System.out.println("--- LEGACY ---");
            System.out.println(legacyCmd);

            throw new RuntimeException("Command difference!!!");
        }
    }

    private Set<String> tokenizeCommand(String s) {
        return new HashSet<>(
            Arrays.asList(s.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
        );
    }
}
