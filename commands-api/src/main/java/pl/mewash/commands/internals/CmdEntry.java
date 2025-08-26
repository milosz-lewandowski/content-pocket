package pl.mewash.commands.internals;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import pl.mewash.commands.settings.response.ResponseProperties;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.BiPredicate;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CmdEntry {
    final YtDlpCmd cmd;
    String param;

    // --- creators ---
    static CmdEntry of(YtDlpCmd cmd) {
        return new CmdEntry(cmd);
    }

    static CmdEntry withParam(YtDlpCmd cmd, String param) {
        paramChecker.test(cmd, param);
        return new CmdEntry(cmd, param);
    }

    // -- finalizers ---
    LinkedList<String> mapToStringList() {
        paramChecker.test(this.cmd, this.param);
        LinkedList<String> list = new LinkedList<>();
        list.add(cmd.getCommand());
        list.add(param);
        return list;
    }

    private static final BiPredicate<YtDlpCmd, String> paramChecker = (dlpCmd, param) -> {
        if (dlpCmd.isParametrized() && param == null)
            throw new IllegalArgumentException("Cannot add Parametrized cmd Without parameter");
        if (!dlpCmd.isParametrized() && param != null)
            throw new IllegalArgumentException("Cannot add Non-Parametrized cmd With parameter");
        return true;
    };
}

