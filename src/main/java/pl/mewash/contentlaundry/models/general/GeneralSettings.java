package pl.mewash.contentlaundry.models.general;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralSettings {

    public boolean binariesDirConfirmed;        // if valid binaries detected
    public String binariesDirPath;              // saves path to directory with detected tools binaries

    public String batchLastSelectedPath;
    public String subsLastSelectedPath;
    public List<Formats> formats;
    public boolean withMetadata;
    public GroupingMode groupingMode;
    public boolean withDateDir;
    public MultithreadingMode multithreadingMode;

    public GeneralSettings() {
    }

    public GeneralSettings(String batchLastSelectedPath) {
        this.batchLastSelectedPath = batchLastSelectedPath;
    }
}
