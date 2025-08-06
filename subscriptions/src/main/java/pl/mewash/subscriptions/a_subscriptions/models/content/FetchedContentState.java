package pl.mewash.subscriptions.a_subscriptions.models.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.mewash.commands.settings.formats.Formats;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchedContentState {
    // properties
    private String id;
    private String title;
    private String url;
    private LocalDateTime published;

    // state
    private boolean fetched;
    private EnumSet<Formats> downloadedFormats;
    private LocalDateTime lastModified;
}
