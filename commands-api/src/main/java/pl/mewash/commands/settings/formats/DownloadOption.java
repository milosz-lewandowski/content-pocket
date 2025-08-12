package pl.mewash.commands.settings.formats;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

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
    String getOptionName();
    String getDirName();
    String getTitleDiff();
}
