package pl.mewash.commands.settings.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public enum ContentProperties implements ResponseProperties {

    CONTENT_TITLE("%(title)s") {
        @Override
        public ContentResponseDto parseResponseToDto(String responseLine) {
            return ContentResponseDto.builder()
                    .title(responseLine)
                    .build();
        }
    },
    CONTENT_PROPERTIES("%(upload_date)s ||| %(title)s ||| %(webpage_url)s ||| %(id)s",  "\\|\\|\\|") {
        @Override
        public ContentResponseDto parseResponseToDto(String responseLine) {

            String[] parts = responseLine.split(this.splitRegex);
            return ContentResponseDto.builder()
                    .publishedDate(LocalDate.parse(parts[0].trim(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .title(parts[1].trim())
                    .url(parts[2].trim())
                    .id(parts[3].trim())
                    .build();
        }
    };

    ContentProperties(String value) {
        this.value = value;
    }

    ContentProperties(String value, String splitRegex) {
        this.value = value;
        this.splitRegex = splitRegex;
    }

    @Getter private final String value;
    protected String splitRegex;

    public abstract ContentResponseDto parseResponseToDto(String responseLine);

    @Getter
    @Builder
    public static final class ContentResponseDto {
        private LocalDate publishedDate;
        private String title;
        private String url;
        private String id;
    }
}
