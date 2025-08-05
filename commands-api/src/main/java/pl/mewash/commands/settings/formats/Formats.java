package pl.mewash.commands.settings.formats;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Formats {
    M4A("m4a", true),
    MP3("mp3", true),
    WAV("wav", true),
    ORIGINAL_SOURCE("original codec", true),
    MP4("mp4",false);

    public final String fileExtension;
    public final boolean audioFormat;

    public String getExtension() {
        return fileExtension;
    }
}
