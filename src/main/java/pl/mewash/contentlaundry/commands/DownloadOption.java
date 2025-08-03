package pl.mewash.contentlaundry.commands;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AudioOnlyQuality.class, name = "audio"),
        @JsonSubTypes.Type(value = VideoQuality.class, name = "video")
})
public sealed interface DownloadOption permits VideoQuality, AudioOnlyQuality{
    String getFormatExtension();
    String getShortDescription();
}
