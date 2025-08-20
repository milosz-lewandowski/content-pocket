package pl.mewash.common.app.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralSettings {

    private boolean binariesDirConfirmed;        // if valid binaries detected
    private String binariesDirPath;              // saves path to directory with detected tools binaries

    private String batchLastSelectedPath;
    private String subsLastSelectedPath;
}
