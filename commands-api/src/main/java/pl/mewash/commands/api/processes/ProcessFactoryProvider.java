package pl.mewash.commands.api.processes;

import pl.mewash.commands.internals.legacy.LegacyProcessFactory;
import pl.mewash.commands.internals.services.DefaultProcessFactory;

import java.util.function.Consumer;

public class ProcessFactoryProvider {

    // --- default ---

    public static ProcessFactory createDefaultFactory(String ytDlpCommandPath, String ffmpegCommandPath) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, false);
    }

    public static ProcessFactory createDefaultFactory(String ytDlpCommandPath, String ffmpegCommandPath,
                                                      boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, printToConsole);
    }

    public static ProcessFactory createDefaultFactoryWithLogger(String ytDlpCommandPath, String ffmpegCommandPath,
                                                                Consumer<String> commandLogger, boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, commandLogger, printToConsole);
    }

    // --- legacy ---

    public static ProcessFactory createLegacyFactory(String ytDlpCommandPath, String ffmpegCommandPath) {
        return new LegacyProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, false);
    }

    public static ProcessFactory createLegacyFactory(String ytDlpCommandPath, String ffmpegCommandPath,
                                                     boolean printToConsole) {
        return new LegacyProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, printToConsole);
    }

    public static ProcessFactory createLegacyFactoryWithLogger(String ytDlpCommandPath, String ffmpegCommandPath,
                                                               Consumer<String> commandLogger, boolean printToConsole) {
        return new LegacyProcessFactory(ytDlpCommandPath, ffmpegCommandPath, commandLogger, printToConsole);
    }


}
