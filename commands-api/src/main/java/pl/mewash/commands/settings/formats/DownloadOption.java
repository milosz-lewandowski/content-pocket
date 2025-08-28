package pl.mewash.commands.settings.formats;

import pl.mewash.commands.api.entries.CmdEntry;

import java.util.List;

public sealed interface DownloadOption permits AudioOption, VideoOption {
    String getOptionName();
    String getDirName();
    String getTitleDiff();
    List<CmdEntry> getCmdEntries();
}
