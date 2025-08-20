package pl.mewash.common.logging.api;

import java.util.List;

public interface FileLogger {

    void logErrStackTrace(Throwable e, boolean serr);
    void logErrWithMessage(String msg, Throwable e, boolean serr);
    void appendSingleLine(String message);
    void appendMultiLineStringList(List<String> lines);
    void consumeAndLogProcessOutputToFile(Process process);
    List<String> getProcessOutputAndLogToFile(Process process);
}
