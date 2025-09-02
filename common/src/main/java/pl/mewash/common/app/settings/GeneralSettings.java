package pl.mewash.common.app.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.mewash.common.app.binaries.BinariesInstallation;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneralSettings {

    private BinariesInstallation binariesInstallation;

    private String batchLastSelectedPath;
    private String subsLastSelectedPath;
}
