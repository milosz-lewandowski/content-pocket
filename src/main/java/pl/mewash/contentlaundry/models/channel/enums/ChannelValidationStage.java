package pl.mewash.contentlaundry.models.channel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelValidationStage {
    NEW("Add", false),
    CHECKING("Checking", true),
    VALIDATED("Add", false);

    private final String buttonTitle;
    private final boolean buttonDisabled;

}
