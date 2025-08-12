package pl.mewash.commands.settings.storage;

import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;

import java.util.Set;

public record StorageOptions(boolean withMetadataFiles,
                             GroupingMode groupingMode,
                             boolean withDownloadedDateDir,
                             boolean multipleVidResolutions,
                             boolean audioNamesConflict) {

    public static StorageOptions withConflictsTest(boolean withMetadata, GroupingMode groupingMode, boolean withDownloadedDateDir,
                                                   Set<DownloadOption> selectedOptions){
        return new StorageOptions(withMetadata, groupingMode, withDownloadedDateDir,
            testMultipleVidResolutions(selectedOptions),
            testAudioConflict(selectedOptions));
    }

    public static StorageOptions getDefaultNoGrouping(Set<DownloadOption> selectedOptions) {
        return new StorageOptions(false, GroupingMode.NO_GROUPING, false,
            testMultipleVidResolutions(selectedOptions),
            testAudioConflict(selectedOptions));
    }

    public static StorageOptions getDefaultWithGrouping(GroupingMode groupingMode, Set<DownloadOption> selectedOptions) {
        return new StorageOptions(false, groupingMode, false,
            testMultipleVidResolutions(selectedOptions),
            testAudioConflict(selectedOptions));
    }

    public static boolean testAudioConflict(Set<DownloadOption> selectedOptions) {
        return AudioOnlyQuality.getConflictsPredicate().test(selectedOptions);
    }

    public static boolean testMultipleVidResolutions(Set<DownloadOption> selectedOptions) {
        return VideoQuality.getConflictsPredicate().test(selectedOptions);
    }
}
