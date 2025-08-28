package pl.mewash.commands.api.entries;

import pl.mewash.commands.settings.cmd.DlpCmd;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.storage.StorageOptions;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CmdBuilder {

    private final String ytDlpBin;
    private final boolean printToConsole;
    private final Consumer<String> commandLogger;
    private final LinkedList<CmdEntry> cmdEntries;

    private CmdBuilder(String ytDlpBin, boolean printToConsole, Consumer<String> commandLogger) {
        this.cmdEntries = new LinkedList<>();
        this.ytDlpBin = ytDlpBin;
        this.commandLogger = commandLogger;
        this.printToConsole = printToConsole;
    }

    public static CmdBuilder newYtDlpCommand(String ytDlpCommandPath) {
        return new CmdBuilder(ytDlpCommandPath, false, null);
    }

    public static CmdBuilder newYtDlpCommand(String ytDlpCommandPath, boolean printToConsole) {
        return new CmdBuilder(ytDlpCommandPath, printToConsole, null);
    }

    public static CmdBuilder newYtDlpCommand(String ytDlpCommandPath,
                                             boolean printToConsole,
                                             Consumer<String> commandLogger) {
        return new CmdBuilder(ytDlpCommandPath, printToConsole, commandLogger);
    }

    public CmdBuilder add(CmdEntry entry) {
        this.cmdEntries.add(entry);
        return this;
    }

    public CmdBuilder add(List<CmdEntry> entriesList) {
        cmdEntries.addAll(entriesList);
        return this;
    }

    public CmdBuilder add(DlpCmd.CmdBundle bundle) {
        cmdEntries.addAll(bundle.getEntries());
        return this;
    }

    public CmdBuilder addOptional(CmdEntry entry, boolean isSelected) {
        if (isSelected) this.cmdEntries.add(entry);
        return this;
    }

    public CmdBuilder addOptional(LinkedList<CmdEntry> entriesList, boolean isSelected) {
        if (isSelected) cmdEntries.addAll(entriesList);
        return this;
    }

    public CmdBuilder addOptional(DlpCmd.CmdBundle bundle, boolean isSelected) {
        if (isSelected) cmdEntries.addAll(bundle.getEntries());
        return this;
    }

    public CmdBuilder setFilePrint(CmdPrintEntry cmdPrintEntry) {
        if (this.cmdEntries.stream()
            .anyMatch((entry) -> entry instanceof CmdPrintEntry)
        ) throw new IllegalStateException("you cannot specify more than one print to file entry");

        this.cmdEntries.add(cmdPrintEntry);
        return this;
    }

    public ProcessBuilder buildDownload(String url, DownloadOption downloadOption, StorageOptions storageOptions) {
        List<CmdEntry> entries = downloadOption.getCmdEntries();
        entries.addAll(OutputPatternResolver.buildCmdEntries(storageOptions, downloadOption));
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
}