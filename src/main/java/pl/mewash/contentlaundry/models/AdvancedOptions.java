package pl.mewash.contentlaundry.models;

public record AdvancedOptions(boolean withMetadata,
                              OutputStructure outputStructure,
                              boolean withDateDir) {
}
