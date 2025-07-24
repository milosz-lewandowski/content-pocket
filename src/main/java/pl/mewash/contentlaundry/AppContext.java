package pl.mewash.contentlaundry;

import lombok.Getter;
import pl.mewash.contentlaundry.utils.BinariesManager;

@Getter
public class AppContext {
    private static final AppContext INSTANCE = new AppContext();
    private boolean initialized = false;

    private String confirmedToolsDir;
    private BinariesManager.SupportedPlatforms detectedPlatform;

    private String YtDlpCommand;
    private String FfMpegCommand;

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
}
