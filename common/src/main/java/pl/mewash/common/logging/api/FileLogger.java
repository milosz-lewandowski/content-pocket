package pl.mewash.common.logging.api;

import java.util.List;

public interface FileLogger {

    void logErrStackTrace(Throwable e, boolean serr);
    void logErrWithMessage(String msg, Throwable e, boolean serr);
    void appendSingleLine(String message);
    void appendMultiLineStringList(List<String> lines);
    void consumeAndLogProcessOutputToFile(Process process);

    @Deprecated
    List<String> getProcessOutputAndLogToFile(Process process);

    /**
     * Create a stateful managed ProcessLogger which writes to shared buffer.
     */
    ProcessLogger getNewProcessLogger();
}
