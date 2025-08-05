package pl.mewash.commands.api;

import pl.mewash.commands.services.DefaultProcessFactory;

public class ProcessFactoryProvider {

    public static ProcessFactory createDefault(String ytDlpCommandPath, String ffmpegCommandPath) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, false);

    }

    public static ProcessFactory createDefaultWithLogger(String ytDlpCommandPath, String ffmpegCommandPath, CommandLogger logger) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, logger, false);

    }

    public static ProcessFactory createDefaultWithConsolePrint(String ytDlpCommandPath, String ffmpegCommandPath, boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, printToConsole);
    }

    public static ProcessFactory createDefaultWithConsolePrintAndLogger(String ytDlpCommandPath, String ffmpegCommandPath,
                                                                        CommandLogger logger, boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, logger, printToConsole);
    }
}
