package pl.mewash.subscriptions.internal.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import pl.mewash.commands.settings.formats.AudioOnlyQuality;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.commands.settings.formats.VideoQuality;
import pl.mewash.commands.settings.response.ContentProperties;
import pl.mewash.subscriptions.internal.domain.state.ContentDownloadStage;

import java.nio.file.Path;
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
    private String channelUrl;

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
        };
    }

    @JsonIgnore
    public void setDownloadingStage(DownloadOption downloadOption) {
        switch (downloadOption) {
            case VideoQuality vq -> videoStage = ContentDownloadStage.SAVING;
            case AudioOnlyQuality va-> audioStage = ContentDownloadStage.SAVING;
        }
    }

    @JsonIgnore
    public void setDownloadingErrorState(DownloadOption downloadOption) {
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

    public static FetchedContent fromContentPropertiesResponse(String propertiesResponse, String channelUrl) {
        ContentProperties contentProperties = ContentProperties.CONTENT_PROPERTIES;
        ContentProperties.ContentResponseDto contentResponseDto = contentProperties.parseResponseToDto(propertiesResponse);


        return FetchedContent.builder()
                .title(contentResponseDto.getTitle())
                .url(contentResponseDto.getUrl())
                .audioStage(ContentDownloadStage.GET)
                .videoStage(ContentDownloadStage.GET)
                .published(LocalDateTime.of(contentResponseDto.getPublishedDate(), LocalTime.MIN))
                .id(contentResponseDto.getId())
                .channelUrl(channelUrl)
                .build();
    }
}
