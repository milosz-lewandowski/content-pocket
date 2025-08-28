package pl.mewash.subscriptions.internal.persistence.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.VideoQuality;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AudioOnlyQuality.class, name = "audio"),
    @JsonSubTypes.Type(value = VideoQuality.class, name = "video")
})
public interface DownloadOptionMixin {}
