package pl.mewash.commands.settings.formats;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pl.mewash.commands.api.entries.CmdEntry;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AudioOnlyQuality.class, name = "audio"),
        @JsonSubTypes.Type(value = VideoQuality.class, name = "video")
})
public sealed interface DownloadOption permits AudioOnlyQuality, VideoQuality {
    String getOptionName();
    String getDirName();
    String getTitleDiff();
    List<CmdEntry> getCmdEntries();
}
