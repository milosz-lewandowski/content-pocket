package pl.mewash.subscriptions.a_subscriptions.models.content;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
