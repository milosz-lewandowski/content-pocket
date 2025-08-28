package pl.mewash.commands.settings.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.cmd.DlpCmd;
import pl.mewash.commands.settings.cmd.DownloadCmd;

import java.util.EnumSet;
import java.util.Optional;

@RequiredArgsConstructor
public enum AdditionalFiles {
    MEDIA_ONLY("Media file only",
        "Saves only media (audio/video) file.",
        EnumSet.of(FileType.MEDIA)),
    MEDIA_WITH_DESCRIPTION("Media with description",
        "Creates a title dir with: media & description files inside.",
        EnumSet.of(FileType.MEDIA, FileType.DESCRIPTION)),
    MEDIA_WITH_METADATA("Media with description & metadata",
        "Creates a title dir with: media, description & info.json files inside.",
        EnumSet.of(FileType.MEDIA, FileType.DESCRIPTION, FileType.INFO_JSON)),
    ;

    @Getter private final String buttonTitle;
    @Getter private final String tooltip;
    @Getter private final EnumSet<FileType> files;

    @RequiredArgsConstructor
    public enum FileType {
        MEDIA(false, "", null),
        DESCRIPTION(true, "description:", DownloadCmd.WRITE_DESC_FILE),
        INFO_JSON(true, "infojson:", DownloadCmd.WRITE_INFO_JSON_FILE),
        ;

        @Getter private final boolean isMetadata;
        @Getter private final String outputTag;
        private final DownloadCmd downloadCmd;

        public Optional<DlpCmd> getDownloadCmd() {
            return Optional.ofNullable(downloadCmd);
        }
    }
}
