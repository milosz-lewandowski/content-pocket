package pl.mewash.commands.api;

import pl.mewash.commands.services.DefaultProcessFactory;

import java.util.function.Consumer;

public class ProcessFactoryProvider {

    public static ProcessFactory createDefault(String ytDlpCommandPath, String ffmpegCommandPath) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, false);

    }

    public static ProcessFactory createDefaultWithLogger(String ytDlpCommandPath, String ffmpegCommandPath, Consumer<String> commandLogger) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, commandLogger, false);

    }

    public static ProcessFactory createDefaultWithConsolePrint(String ytDlpCommandPath, String ffmpegCommandPath, boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, null, printToConsole);
    }

    public static ProcessFactory createDefaultWithConsolePrintAndLogger(String ytDlpCommandPath, String ffmpegCommandPath,
                                                                        Consumer<String> commandLogger, boolean printToConsole) {
        return new DefaultProcessFactory(ytDlpCommandPath, ffmpegCommandPath, commandLogger, printToConsole);
    }
}
