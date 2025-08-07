package pl.mewash.commands.settings.storage;

public record StorageOptions(boolean withMetadataFiles,
                             GroupingMode groupingMode,
                             boolean withDownloadedDateDir) {

    public static StorageOptions getDefaultNoGrouping() {
        return new StorageOptions(false, GroupingMode.NO_GROUPING, false);
    }

    public static StorageOptions getDefaultWithGrouping(GroupingMode groupingMode) {
        return new StorageOptions(false, groupingMode, false);
    }
}
