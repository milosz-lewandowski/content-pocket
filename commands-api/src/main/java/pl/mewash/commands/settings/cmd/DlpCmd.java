package pl.mewash.commands.settings.cmd;

import pl.mewash.commands.api.entries.CmdEntry;

import java.util.LinkedList;

public interface DlpCmd {
    String getCommand();
    boolean isParametrized();

    interface CmdBundle {
        LinkedList<CmdEntry> getEntries();
    }
}