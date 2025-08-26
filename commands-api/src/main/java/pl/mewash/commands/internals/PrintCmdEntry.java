package pl.mewash.commands.internals;

import pl.mewash.commands.settings.response.ResponseProperties;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.BiPredicate;

public class PrintCmdEntry extends CmdEntry {
    final Path filePath;

    private PrintCmdEntry(ResponseProperties responseProperties, Path filePath) {
        super(FetchCmd.PRINT_TO_FILE, responseProperties.getPattern());
        this.filePath = filePath;
    }

    static PrintCmdEntry withResponsePropsAndFile(ResponseProperties responseProperties, Path filePath) {
        paramChecker.test(responseProperties, filePath);
        return new PrintCmdEntry(responseProperties, filePath);
    }

    @Override
    LinkedList<String> mapToStringList() {
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
