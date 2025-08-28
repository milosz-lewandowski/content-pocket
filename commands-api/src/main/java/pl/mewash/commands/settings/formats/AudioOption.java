package pl.mewash.commands.settings.formats;

public non-sealed interface AudioOption extends DownloadOption {
    boolean getNeedsFFmpeg();
    boolean getCanEmbedMetadata();
}
