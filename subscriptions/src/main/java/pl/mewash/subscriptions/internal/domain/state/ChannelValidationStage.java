package pl.mewash.subscriptions.internal.domain.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelValidationStage {
    ADD_NEW("Add", false),
    CHECKING("Checking", true)
    ;

    private final String buttonTitle;
    private final boolean buttonDisabled;

}
