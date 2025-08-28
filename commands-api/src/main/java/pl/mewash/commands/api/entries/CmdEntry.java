package pl.mewash.commands.api.entries;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.cmd.DlpCmd;

import java.util.LinkedList;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CmdEntry {
    protected final DlpCmd cmd;
    protected String param;

    public static CmdEntry of(DlpCmd cmd) {
        return new CmdEntry(cmd);
    }

    public static CmdEntry withParam(DlpCmd cmd, String param) {
        validateOrThrow(cmd, param);
        return new CmdEntry(cmd, param);
    }

    public LinkedList<String> mapToStringList() {
        validateOrThrow(cmd, param);
        LinkedList<String> list = new LinkedList<>();
        list.add(cmd.getCommand());
        if (param != null) list.add(param);
        return list;
    }

    private static void validateOrThrow(DlpCmd cmd, String param) {
        if (cmd.isParametrized() && param == null)
            throw new IllegalArgumentException("Cannot add Parametrized cmd Without parameter");
        if (!cmd.isParametrized() && param != null)
            throw new IllegalArgumentException("Cannot add Non-Parametrized cmd With parameter");
    }
}

