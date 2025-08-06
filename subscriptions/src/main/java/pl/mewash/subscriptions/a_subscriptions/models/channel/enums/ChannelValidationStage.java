package pl.mewash.subscriptions.a_subscriptions.models.channel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelValidationStage {
    ADD_NEW("Add", false),
    CHECKING("Checking", true)
    ;

    private final String buttonTitle;
    private final boolean buttonDisabled;

}
