package pl.mewash.common;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AppContext {
    private static final AppContext INSTANCE = new AppContext();
    private boolean initialized = false;

    private String confirmedToolsDir;
    private BinariesManager.SupportedPlatforms detectedPlatform;

    private String YtDlpCommand;
    private String FfMpegCommand;

    private final List<OnCloseHandler> onCloseHandlers = new ArrayList<>();

    private AppContext() {}

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public void init(BinariesManager.SupportedPlatforms platform, String toolsDir) {
        if (initialized) {
            throw new IllegalStateException("AppContext already initialized.");
        }
        this.detectedPlatform = platform;
        this.confirmedToolsDir = toolsDir;

        this.YtDlpCommand = BinariesManager.BinariesNames.
                YT_DLP.getPathByLocation(toolsDir, platform).toAbsolutePath().toString();
        this.FfMpegCommand = BinariesManager.BinariesNames.
                FFMPEG.getPathByLocation(toolsDir, platform).toAbsolutePath().toString();

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
}
