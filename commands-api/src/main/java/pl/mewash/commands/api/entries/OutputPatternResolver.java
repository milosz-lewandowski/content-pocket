package pl.mewash.commands.api.entries;

import pl.mewash.commands.settings.cmd.DownloadCmd;
import pl.mewash.commands.settings.formats.AudioOption;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoOption;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

public class OutputPatternResolver {

    public static List<CmdEntry> buildCmdEntries(StorageOptions storageOptions, DownloadOption downloadOption) {
        return storageOptions.additionalFiles().getFileTypes()
            .stream()
            .flatMap(fileType -> {
                String outputPattern = buildStoragePathPattern(storageOptions, downloadOption, fileType.isMetadata());
                Stream<CmdEntry> patternEntry = Stream.of(CmdEntry
                    .withParam(DownloadCmd.OUTPUT_TEMPLATE, fileType.getOutputTag() + outputPattern));
                Stream<CmdEntry> downloadEntry = fileType.getDownloadCmd().stream()
                    .map(CmdEntry::of);
                return Stream.concat(patternEntry, downloadEntry);
            })
            .toList();
    }

    public static String buildMedia(StorageOptions storageOptions, DownloadOption downloadOption) {
        return buildStoragePathPattern(storageOptions, downloadOption, false);
    }

    public static String buildMetadata(StorageOptions storageOptions, DownloadOption downloadOption) {
        return buildStoragePathPattern(storageOptions, downloadOption, true);
    }

    private static String buildStoragePathPattern(StorageOptions storageOptions, DownloadOption downloadOption,
                                                  boolean isMetadataFilePattern) {

        String diffedFormatDir = switch (downloadOption) {
            case VideoOption vq -> storageOptions.multipleVidResolutions()
                ? vq.getDirName() + "/" + vq.getResolution() + "p" + "/"
                : vq.getDirName() + "/";
            case AudioOption aq -> aq.getDirName() + "/";
        };

        String dateDir = storageOptions.withDownloadedDateDir() ? LocalDate.now() + "/" : "";

        String titleDir = "%(title)s" + "/";
        String metadataDir = storageOptions.additionalFiles() == AdditionalFiles.MEDIA_ONLY
            ? ""
            : titleDir;

        String pureTitle = "%(title)s.%(ext)s";
        String diffedTitle = isMetadataFilePattern
            ? pureTitle
            : switch (downloadOption) {
            case VideoOption vq -> storageOptions.multipleVidResolutions()
                ? "%(title)s" + vq.getTitleDiff() + ".%(ext)s"
                : pureTitle;
            case AudioOption aq -> storageOptions.audioNamesConflict()
                ? "%(title)s" + aq.getTitleDiff() + ".%(ext)s"
                : pureTitle;
        };

        return switch (storageOptions.groupingMode()) {

            // distinct at format dir level
            case GROUP_BY_FORMAT -> diffedFormatDir + dateDir + metadataDir + pureTitle;

            // distinct at title level
            case GROUP_BY_CONTENT -> dateDir + titleDir + diffedTitle;

            // distinct at metadata dir or title level
            case NO_GROUPING -> dateDir + metadataDir + diffedTitle;
        };
    }
}
