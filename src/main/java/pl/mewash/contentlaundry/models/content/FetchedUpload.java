package pl.mewash.contentlaundry.models.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public final class FetchedUpload {
    private String title;
    private String url;
    private LocalDateTime published;
    private String id;
    private String channelName;
}
