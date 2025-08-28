package pl.mewash.commands.api.processes;

import pl.mewash.commands.settings.formats.AudioOption;
import pl.mewash.commands.settings.formats.VideoOption;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface ProcessFactory {

    ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile);

    ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate,
                                               ContentProperties contentProperties, Path tempFile);

    ProcessBuilder downloadAudioStream(String url, AudioOption audioOption,
                                       StorageOptions storageOptions, Path tempFile);

    ProcessBuilder downloadVideoWithAudioStream(String url, VideoOption videoOption,
                                                StorageOptions storageOptions, Path tempFile);
}
