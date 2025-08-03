package pl.mewash.contentlaundry.models.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import pl.mewash.contentlaundry.commands.AudioOnlyQuality;
import pl.mewash.contentlaundry.commands.CommandBuilder;
import pl.mewash.contentlaundry.commands.DownloadOption;
import pl.mewash.contentlaundry.commands.VideoQuality;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class FetchedContent {

    private String title;
    private String displayTitle;
    private String url;
    private LocalDateTime published;
    private String id;
    private String channelName;

    private ContentDownloadStage audioStage;
    private String audioPath;
    private ContentDownloadStage videoStage;
    private String videoPath;

    @Builder.Default
    private Set<DownloadOption> downloadedAs = new HashSet<>();
    private LocalDateTime lastUpdated;

    public void addAndSetDownloaded(DownloadOption downloadOption, Path downloadedPath) {
        downloadedAs.add(downloadOption);
        switch (downloadOption) {
            case VideoQuality vq -> {
                videoStage = ContentDownloadStage.SAVED;
                videoPath = downloadedPath.toAbsolutePath().toString();
            }
            case AudioOnlyQuality va-> {
                audioStage = ContentDownloadStage.SAVED;
                audioPath = downloadedPath.toAbsolutePath().toString();
            }
        }
    }

    @JsonIgnore
    public boolean isDownloaded(DownloadOption downloadOption) {
        return switch (downloadOption) {
            case VideoQuality vq -> videoStage == ContentDownloadStage.SAVED;
            case AudioOnlyQuality va-> audioStage == ContentDownloadStage.SAVED;
            default -> throw new IllegalArgumentException("Invalid download option");
        };
    }

//    @JsonIgnore
//    public boolean isAudioDownloaded(){
//        return downloadedAs.stream().anyMatch(option -> option instanceof AudioOnlyQuality);
//    }

    @JsonIgnore
    public void setDownloadingStage(DownloadOption downloadOption) {
        switch (downloadOption) {
            case VideoQuality vq -> videoStage = ContentDownloadStage.SAVING;
            case AudioOnlyQuality va-> audioStage = ContentDownloadStage.SAVING;
        }
    }

    @JsonIgnore
    public void setDownloadingError(DownloadOption downloadOption) {
        switch (downloadOption) {
            case VideoQuality vq -> videoStage = ContentDownloadStage.ERROR;
            case AudioOnlyQuality va-> audioStage = ContentDownloadStage.ERROR;
        }
    }

    @JsonIgnore
    public FetchedContent updateDisplayTitle(boolean yearDate){
        DateTimeFormatter formatter = yearDate
                ? DateTimeFormatter.ofPattern("d.MM.yy")
                : DateTimeFormatter.ofPattern("d.MM");

        this.displayTitle = "(" + formatter.format(getPublished()) + ") " + getTitle();
        return this;
    }

    public static FetchedContent fromContentPropertiesResponse(String propertiesResponse, String channelName) {
        String splitRegex = CommandBuilder.PrintToFileOptions.CONTENT_PROPERTIES.getSplitRegex();
        String[] parts = propertiesResponse.split(splitRegex);
//        String[] parts = propertiesResponse.split("\\|\\|\\|");

        String dateString = parts[0].trim();
        String title = parts[1].trim();
        String url = parts[2].trim();
        String id = parts[3].trim();

        LocalDate publishedDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDateTime publishedDateTime = LocalDateTime.of(publishedDate, LocalTime.MIN);

        return FetchedContent.builder()
                .title(title)
                .url(url)
                .audioStage(ContentDownloadStage.GET)
                .videoStage(ContentDownloadStage.GET)
                .published(publishedDateTime)
                .id(id)
                .channelName(channelName)
                .build();
    }
}
