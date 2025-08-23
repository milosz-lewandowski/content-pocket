package pl.mewash.commands.settings.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdditionalFiles {
    MEDIA_ONLY("Media file only",
        "Saves only media (audio/video) file."),
    MEDIA_WITH_DESCRIPTION("Media with description",
        "Creates a title dir with: media & description files inside."),
    MEDIA_WITH_METADATA("Media with description & metadata",
        "Creates a title dir with: media, description & info.json files inside.");

    private final String buttonTitle;
    private final String tooltip;
}
