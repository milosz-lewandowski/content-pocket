package pl.mewash.batch.internals.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BatchProcessingState {
    NOT_RUNNING(false, "Start Laundry"),
    PROCESSING(false, "Stop Laundry"),
    IN_GRACEFUL_SHUTDOWN(false, "Stopping nicely... Force Shutdown?"),
    IN_FORCED_SHUTDOWN(true, "Forcing Shutdown!"),

    ;
    @Getter private final boolean buttonDisabled;
    @Getter private final String buttonTitle;
}
