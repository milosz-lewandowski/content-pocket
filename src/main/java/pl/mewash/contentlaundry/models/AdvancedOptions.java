package pl.mewash.contentlaundry.models;

import pl.mewash.contentlaundry.utils.MultithreadingMode;
import pl.mewash.contentlaundry.utils.OutputStructure;

public record AdvancedOptions(boolean withMetadata,
                              OutputStructure outputStructure,
                              boolean withDateDir,
                              MultithreadingMode multithreadingMode) {
}
