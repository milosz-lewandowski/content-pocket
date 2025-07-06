package pl.mewash.contentlaundry.models.general;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralSettings {

    public BinariesConfig binariesConfig;

    public boolean alreadyConfirmed;
    public String dirPath;

    public String toolsBinariesPath;

    public String lastSelectedPath;
    public List<Formats> formats;
    public boolean withMetadata;
    public GroupingMode groupingMode;
    public boolean withDateDir;
    public MultithreadingMode multithreadingMode;

    public GeneralSettings() {
    }

    public GeneralSettings(String lastSelectedPath) {
        this.lastSelectedPath = lastSelectedPath;
    }
}
