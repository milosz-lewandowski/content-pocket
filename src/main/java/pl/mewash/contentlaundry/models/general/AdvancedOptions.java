package pl.mewash.contentlaundry.models.general;

import pl.mewash.contentlaundry.models.general.enums.GroupingMode;
import pl.mewash.contentlaundry.models.general.enums.MultithreadingMode;

public record AdvancedOptions(boolean withMetadata,
                              GroupingMode groupingMode,
                              boolean withDateDir,
                              MultithreadingMode multithreadingMode) {
}
