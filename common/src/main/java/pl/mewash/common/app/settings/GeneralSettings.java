package pl.mewash.common.app.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import pl.mewash.commands.settings.storage.GroupingMode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralSettings {

    public boolean binariesDirConfirmed;        // if valid binaries detected
    public String binariesDirPath;              // saves path to directory with detected tools binaries

    public String batchLastSelectedPath;
    public String subsLastSelectedPath;
    public boolean withMetadata;
    public GroupingMode groupingMode;
    public boolean withDateDir;

    public GeneralSettings() {
    }

    public GeneralSettings(String batchLastSelectedPath) {
        this.batchLastSelectedPath = batchLastSelectedPath;
    }
}
