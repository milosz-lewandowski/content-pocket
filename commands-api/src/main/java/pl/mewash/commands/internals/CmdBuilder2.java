package pl.mewash.commands.internals;

import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.storage.AdditionalFiles;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CmdBuilder2 {

    // initial
    private final String ytDlpBin;
    private final boolean printToConsole;
    private final Consumer<String> commandLogger;
    // intermediate
    private final LinkedList<CmdEntry> cmdEntries;
    // finalize
    private String url;


    private CmdBuilder2(String ytDlpBin, boolean printToConsole, Consumer<String> commandLogger) {
        this.cmdEntries = new LinkedList<>();
        this.ytDlpBin = ytDlpBin;
        this.commandLogger = commandLogger;
        this.printToConsole = printToConsole;
    }

    public static CmdBuilder2 newYtDlpCommand(String ytDlpCommandPath) {
        return new CmdBuilder2(ytDlpCommandPath, false, null);
    }

    public static CmdBuilder2 newYtDlpCommand(String ytDlpCommandPath, boolean printToConsole) {
        return new CmdBuilder2(ytDlpCommandPath, printToConsole, null);
    }

    public static CmdBuilder2 newYtDlpCommand(String ytDlpCommandPath,
                                              boolean printToConsole,
                                              Consumer<String> commandLogger) {
        return new CmdBuilder2(ytDlpCommandPath, printToConsole, commandLogger);
    }

    public CmdBuilder2 add(CmdEntry entry) {
        this.cmdEntries.add(entry);
        return this;
    }

    public CmdBuilder2 add(List<CmdEntry> entriesList) {
        cmdEntries.addAll(entriesList);
        return this;
    }

    public CmdBuilder2 add(YtDlpCmd.CmdBundle bundle) {
        cmdEntries.addAll(bundle.getEntries());
        return this;
    }

    public CmdBuilder2 addOptional(CmdEntry entry, boolean isSelected) {
        if (isSelected) this.cmdEntries.add(entry);
        return this;
    }

    public CmdBuilder2 addOptional(LinkedList<CmdEntry> entriesList, boolean isSelected) {
        cmdEntries.addAll(entriesList);
        return this;
    }

    public CmdBuilder2 addOptional(YtDlpCmd.CmdBundle bundle, boolean isSelected) {
        cmdEntries.addAll(bundle.getEntries());
        return this;
    }

    public CmdBuilder2 setFilePrint(PrintCmdEntry printCmdEntry) {
        if (this.cmdEntries.stream()
            .anyMatch((entry) -> entry instanceof PrintCmdEntry)
        ) throw new IllegalStateException("you cannot specify more than one print to file entry");

        this.cmdEntries.add(printCmdEntry);
        return this;
    }

    public ProcessBuilder buildDownload(String url, DownloadOption option, StorageOptions storageOptions) {
        List<CmdEntry> entries = switch (option) {
            case OnlyAudioStream oas -> oas.getDownloadConversionCommands();
            case VideoAudioStream vas -> vas.getDownloadCommand();
            case null, default -> throw new IllegalStateException("unsupported download option");
        };
        entries.addAll(resolveStorageOutputPatterns(storageOptions, option));
        this.cmdEntries.addAll(entries);
        return build(url);
    }

    public ProcessBuilder buildFetch(String url) {
        return build(url);
    }

    private ProcessBuilder build(String url) {
        if (url == null) throw new IllegalStateException("you must specify an url");

        LinkedList<String> allEntriesParametersOrdered = cmdEntries.stream()
            .flatMap(entry -> entry.mapToStringList().stream())
            .collect(Collectors.toCollection(LinkedList::new));

        allEntriesParametersOrdered.addFirst(ytDlpBin);
        allEntriesParametersOrdered.addLast(url);

        if (printToConsole) System.out.println(String.join(" ", allEntriesParametersOrdered));
        if (commandLogger != null) commandLogger.accept(String.join(" ", allEntriesParametersOrdered));
        return new ProcessBuilder(allEntriesParametersOrdered);
    }

    private static List<CmdEntry> resolveStorageOutputPatterns(StorageOptions storageOptions, DownloadOption downloadOption) {
        String mediaTemplate = buildStoragePathPattern(storageOptions, downloadOption, false);
        String metadataTemplate = buildStoragePathPattern(storageOptions, downloadOption, true);

        return switch (storageOptions.additionalFiles()){
            case MEDIA_ONLY -> List.of(
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, mediaTemplate)
            );
            case MEDIA_WITH_DESCRIPTION -> List.of(
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, mediaTemplate),
                CmdEntry.of(DownloadCmd.WRITE_DESC_FILE),
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, "description:" + metadataTemplate)
            );
            case MEDIA_WITH_METADATA -> List.of(
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, mediaTemplate),
                CmdEntry.of(DownloadCmd.WRITE_DESC_FILE),
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, "description:" + metadataTemplate),
                CmdEntry.of(DownloadCmd.WRITE_INFO_JSON_FILE),
                CmdEntry.withParam(DownloadCmd.OUTPUT_TEMPLATE, "infojson:" + metadataTemplate)
            );
        };
    }

    private static String buildStoragePathPattern(StorageOptions storageOptions, DownloadOption downloadOption,
                                                  boolean metadataFilePattern) {

        String diffedFormatDir = switch (downloadOption) {
            case VideoQuality vq -> storageOptions.multipleVidResolutions()
                ? vq.getDirName() + "/" + vq.getResolution() + "p" + "/"
                : vq.getDirName() + "/";
            case AudioOnlyQuality aq -> aq.getDirName() + "/";
            default -> throw new IllegalStateException("unsupported download option");
        };

        String dateDir = storageOptions.withDownloadedDateDir() ? LocalDate.now() + "/" : "";

        String titleDir = "%(title)s" + "/";
        String metadataDir = storageOptions.additionalFiles() == AdditionalFiles.MEDIA_ONLY
            ? ""
            : titleDir;

        String pureTitle = "%(title)s.%(ext)s";
        String diffedTitle = metadataFilePattern
            ? pureTitle
            : switch (downloadOption) {
            case VideoQuality vq -> storageOptions.multipleVidResolutions()
                ? "%(title)s" + vq.getTitleDiff() + ".%(ext)s"
                : pureTitle;
            case AudioOnlyQuality aq -> storageOptions.audioNamesConflict()
                ? "%(title)s" + aq.getTitleDiff() + ".%(ext)s"
                : pureTitle;
            default -> throw new IllegalStateException("unsupported download option");
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