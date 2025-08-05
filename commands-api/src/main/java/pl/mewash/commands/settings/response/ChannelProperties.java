package pl.mewash.commands.settings.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public enum ChannelProperties implements ResponseProperties {
    CHANNEL_NAME("%(channel)s") {
        @Override
        public ChannelResponseDto parseResponseToDto(String responseLine) {

            return ChannelResponseDto.builder()
                    .channelName(responseLine)
                    .build();
        }
    },
    CHANNEL_NAME_LATEST_CONTENT("%(channel)s ||| %(upload_date)s", "\\|\\|\\|") {
        @Override
        public ChannelResponseDto parseResponseToDto(String responseLine) {
            String[] lines = responseLine.split(this.splitRegex);

            return ChannelResponseDto.builder()
                    .channelName(lines[0].trim())
                    .latestContentDate(LocalDate.parse(lines[1].trim(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .build();
        }
    };

    ChannelProperties(String value) {
        this.value = value;
    }

    ChannelProperties(String value, String splitRegex) {
        this.value = value;
        this.splitRegex = splitRegex;
    }

    @Getter private final String value;
    protected String splitRegex;

    public abstract ChannelResponseDto parseResponseToDto(String responseLine);

    @Getter
    @Builder
    public static final class ChannelResponseDto {
        private String channelName;
        private LocalDate latestContentDate;
    }


}
