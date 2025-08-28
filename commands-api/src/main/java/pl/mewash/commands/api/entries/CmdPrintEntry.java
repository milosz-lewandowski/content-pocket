package pl.mewash.commands.api.entries;

import pl.mewash.commands.settings.cmd.FetchCmd;
import pl.mewash.commands.settings.response.ResponseProperties;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.BiPredicate;

public class CmdPrintEntry extends CmdEntry {
    private final Path filePath;

    private CmdPrintEntry(ResponseProperties responseProperties, Path filePath) {
        super(FetchCmd.PRINT_TO_FILE, responseProperties.getPattern());
        this.filePath = filePath;
    }

    public static CmdPrintEntry withResponsePropsAndFile(ResponseProperties responseProperties, Path filePath) {
        paramChecker.test(responseProperties, filePath);
        return new CmdPrintEntry(responseProperties, filePath);
    }

    @Override
    public LinkedList<String> mapToStringList() {
        LinkedList<String> list = super.mapToStringList();
        list.add(filePath.toAbsolutePath().toString());
        return list;
    }

    private static final BiPredicate<ResponseProperties, Path> paramChecker = (respProps, path) -> {
        if (respProps == null || respProps.getPattern() == null)
            throw new IllegalArgumentException("File print Response Pattern cannot be null");
        if (path == null)
            throw new IllegalArgumentException("Path to file print output cannot be null");
        return true;
    };
}
