package pl.mewash.common.logging.api;

import pl.mewash.common.logging.impl.ScheduledFileLogger;

public final class LoggersProvider {

    public static FileLogger getFileLogger(){
        return ScheduledFileLogger.getInstance();
    }
}
