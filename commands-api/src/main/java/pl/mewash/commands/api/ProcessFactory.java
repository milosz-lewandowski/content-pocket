package pl.mewash.commands.api;

import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.storage.StorageOptions;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ChannelProperties;
import pl.mewash.commands.settings.response.ContentProperties;

import java.nio.file.Path;
import java.time.LocalDateTime;

public interface ProcessFactory {

    ProcessBuilder fetchChannelBasicData(String channelUrl, ChannelProperties channelProperties, Path tempFile);

    ProcessBuilder fetchContentsPublishedAfter(String channelUrl, LocalDateTime afterDate,
                                               ContentProperties contentProperties, Path tempFile);

    ProcessBuilder downloadAudioStream(String url, AudioOnlyQuality audioQuality,
                                       StorageOptions storageOptions, Path tempFile);

    ProcessBuilder downloadVideoWithAudioStream(String url, VideoQuality videoQuality,
                                                StorageOptions storageOptions, Path tempFile);
}
