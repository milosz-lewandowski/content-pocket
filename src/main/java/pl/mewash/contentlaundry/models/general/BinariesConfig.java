package pl.mewash.contentlaundry.models.general;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinariesConfig {
    public boolean alreadyConfirmed;
    public String dirPath;

    public BinariesConfig() {
    }
}
