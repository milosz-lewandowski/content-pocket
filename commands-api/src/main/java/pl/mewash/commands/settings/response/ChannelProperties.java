package pl.mewash.commands.settings.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
public enum ChannelProperties implements ResponseProperties {
    CHANNEL_NAME("%(channel)s") {
        @Override
        public ChannelResponseDto parseResponseToDto(String responseLine) {

            return ChannelResponseDto.builder()
                    .channelName(responseLine)
                    .build();
        }
    },
    CHANNEL_NAME_LATEST_CONTENT("%(channel)s ||| %(upload_date)s") {
        @Override
        public ChannelResponseDto parseResponseToDto(String responseLine) {
            String[] lines = responseLine.split(splitRegex);

            return ChannelResponseDto.builder()
                    .channelName(lines[0].trim())
                    .latestContentDate(LocalDate.parse(lines[1].trim(), DateTimeFormatter.ofPattern("yyyyMMdd")))
                    .build();
        }
    };

    @Getter private final String pattern;

    private static final String splitRegex = "\\|\\|\\|";

    public abstract ChannelResponseDto parseResponseToDto(String responseLine);

    @Getter
    @Builder
    public static final class ChannelResponseDto {
        private String channelName;
        private LocalDate latestContentDate;
    }


}
