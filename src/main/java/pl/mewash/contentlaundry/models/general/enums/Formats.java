package pl.mewash.contentlaundry.models.general.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Formats {
    MP3("mp3", true),
    WAV("wav", true),
    MP4("mp4",false);

    public final String value;
    public final boolean audioFormat;
}
