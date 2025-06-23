package pl.mewash.contentlaundry.models.general;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;

@Getter
@Setter
@AllArgsConstructor
public class SubscriptionsTabSettings {
    private String defaultTargetPath;
    private MultithreadingMode multithreadingMode;

    public MultithreadingMode getDefaultMultithreadingMode() {
        return MultithreadingMode.MEDIUM;
    }
}
