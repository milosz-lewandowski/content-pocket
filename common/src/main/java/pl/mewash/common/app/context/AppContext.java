package pl.mewash.common.app.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.mewash.common.app.binaries.BinariesInstallation;
import pl.mewash.common.app.binaries.BinariesNames;
import pl.mewash.common.app.binaries.SupportedPlatforms;
import pl.mewash.common.app.lifecycle.OnCloseHandler;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.LoggersProvider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.UnaryOperator;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppContext {
    // singleton
    private static final AppContext INSTANCE = new AppContext();
    private boolean initialized = false;
    public static AppContext getInstance() {
        return INSTANCE;
    }

    // fields
    private SupportedPlatforms detectedPlatform;
    private String YtDlpCommand;
    private String FfMpegCommand;
    private FileLogger fileLogger;
    private final List<OnCloseHandler> onCloseHandlers = new ArrayList<>();

    public void init(BinariesInstallation binariesInstallation) {
        if (initialized) throw new IllegalStateException("AppContext already initialized.");

        this.detectedPlatform = binariesInstallation.getPlatform();
        this.YtDlpCommand = binariesInstallation.getBinaryCommand(BinariesNames.YT_DLP);
        this.FfMpegCommand = binariesInstallation.getBinaryCommand(BinariesNames.FFMPEG);
        this.fileLogger = LoggersProvider.getFileLogger();
        this.initialized = true;
    }

    public void registerOnCloseHandler(OnCloseHandler onCloseHandler) {
        onCloseHandlers.add(onCloseHandler);
    }

    public void executeOnCloseHandlers() {
        for (OnCloseHandler onCloseHandler : onCloseHandlers) {
            try {
                onCloseHandler.onClose();
            } catch (Exception e) {
                System.err.println("On close handler error: " + e.getMessage());
            }
        }
    }

    public UnaryOperator<String> getResource = (resource) -> new String(Base64.getDecoder().decode(resource),
        StandardCharsets.UTF_8);
}
