package pl.mewash.commands.api.entries;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.cmd.DlpCmd;

import java.util.LinkedList;
import java.util.function.BiPredicate;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CmdEntry {
    protected final DlpCmd cmd;
    protected String param;

    // --- creators ---
    public static CmdEntry of(DlpCmd cmd) {
        return new CmdEntry(cmd);
    }

    public static CmdEntry withParam(DlpCmd cmd, String param) {
        paramChecker.test(cmd, param);
        return new CmdEntry(cmd, param);
    }

    // -- finalizers ---
    public LinkedList<String> mapToStringList() {
        paramChecker.test(this.cmd, this.param);
        LinkedList<String> list = new LinkedList<>();
        list.add(cmd.getCommand());
        if (param != null )list.add(param);
        return list;
    }

    private static final BiPredicate<DlpCmd, String> paramChecker = (dlpCmd, param) -> {
        if (dlpCmd.isParametrized() && param == null)
            throw new IllegalArgumentException("Cannot add Parametrized cmd Without parameter");
        if (!dlpCmd.isParametrized() && param != null)
            throw new IllegalArgumentException("Cannot add Non-Parametrized cmd With parameter");
        return true;
    };
}

