package pl.mewash.commands.settings.formats;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Formats {
    M4A("m4a"),
    MP3("mp3"),
    WAV("wav"),
    ORIGINAL_SOURCE("original codec"),
    MP4("mp4");

    private final String fileExtension;

    public String getExtension() {
        return fileExtension;
    }
}
