package pl.mewash.subscriptions.internal.domain.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelValidationStage {
    ADD_NEW("Add Channel", false),
    CHECKING("Checking...", true)
    ;

    private final String buttonTitle;
    private final boolean buttonDisabled;

}
