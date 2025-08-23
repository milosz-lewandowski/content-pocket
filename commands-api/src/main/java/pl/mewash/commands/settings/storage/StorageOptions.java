package pl.mewash.commands.settings.storage;

import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;

import java.util.Set;

public record StorageOptions(AdditionalFiles additionalFiles,
                             GroupingMode groupingMode,
                             boolean withDownloadedDateDir,
                             boolean multipleVidResolutions,
                             boolean audioNamesConflict) {

    public static StorageOptions withConflictsTest(AdditionalFiles additionalFiles,
                                                   GroupingMode groupingMode,
                                                   boolean withDownloadedDateDir,
                                                   Set<DownloadOption> selectedOptions){
        return new StorageOptions(additionalFiles, groupingMode, withDownloadedDateDir,
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
