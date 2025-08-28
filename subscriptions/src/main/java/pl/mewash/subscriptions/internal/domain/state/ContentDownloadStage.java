package pl.mewash.subscriptions.internal.domain.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContentDownloadStage {
    GET(false, "Get Audio", "Get Video"),
    SAVING(true, "Saving Audio", "Saving Video"),
    SAVED(false, "Open Audio", "Open Video"),
    ERROR(false, "Audio Error", "Video Error");
    ;
    final boolean disabled;
    final String audioTitle;
    final String videoTitle;
}
