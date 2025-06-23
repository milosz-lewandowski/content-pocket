package pl.mewash.contentlaundry.models.channel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChannelFetchingStage {
    TO_FETCH("Fetch", false),
    FETCHING("Fetching", true),
    FETCHED("Fetch older", false),;


    private final String buttonTitle;
    private final boolean isDisabled;
}
