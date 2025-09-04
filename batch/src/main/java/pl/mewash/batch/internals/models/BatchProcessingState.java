package pl.mewash.batch.internals.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BatchProcessingState {
    NOT_RUNNING(false, "Start processing"),
    PROCESSING(false, "Stop processing"),
    IN_GRACEFUL_SHUTDOWN(false, "Stopping nicely... Force Shutdown?"),
    IN_FORCED_SHUTDOWN(true, "Forcing Shutdown!");

    @Getter private final boolean buttonDisabled;
    @Getter private final String buttonTitle;
}
