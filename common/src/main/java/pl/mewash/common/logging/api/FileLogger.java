package pl.mewash.common.logging.api;

import java.util.List;

public interface FileLogger {

    void logErrStackTrace(Throwable e, boolean serr);
    void logErrWithMessage(String msg, Throwable e, boolean serr);
    void appendSingleLine(String message);
    void appendMultiLineStringList(List<String> lines);
    void consumeAndLogProcessOutputToFile(Process process);
    // FIXME: returning output from logger (overwhelmed with responsibilities atm) is a temporary solution before
    // TODO: introducing real-time process manager with output analyzer, watchdogs, triggers, etc.
    List<String> getProcessOutputAndLogToFile(Process process);
}
