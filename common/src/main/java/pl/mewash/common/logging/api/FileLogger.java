package pl.mewash.common.logging.api;

import java.util.List;

public interface FileLogger {
    void appendSingleLine(String message);
    void appendMultiLineStringList(List<String> lines);
    void consumeAndLogProcessOutputToFile(Process process);
}
