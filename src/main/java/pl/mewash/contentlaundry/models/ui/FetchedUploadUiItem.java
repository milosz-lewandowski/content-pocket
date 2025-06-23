package pl.mewash.contentlaundry.models.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import pl.mewash.contentlaundry.models.content.FetchedUpload;

@Getter
@AllArgsConstructor
public class FetchedUploadUiItem {
    private final FetchedUpload fetchedUpload;
    @Setter
    private boolean selected;
}
