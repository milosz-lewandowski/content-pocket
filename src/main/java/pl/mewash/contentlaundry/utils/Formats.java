package pl.mewash.contentlaundry.utils;

public enum Formats {
    MP3("mp3", true),
    WAV("wav", true),
    MP4("mp4",false);

    public final String value;
    public final boolean audioFormat;

    Formats(String value, boolean AudioFormat) {
        this.value = value;
        this.audioFormat = AudioFormat;
    }
}
