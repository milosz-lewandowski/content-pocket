package pl.mewash.common.downloads.api;

import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.impl.DefaultDownloadService;

public final class DownloadServiceProvider {

    public static DownloadService getDefaultDownloadService(AppContext appContext) {
        return new DefaultDownloadService(appContext);
    }
}
