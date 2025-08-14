package pl.mewash.common.downloads.api;

import pl.mewash.common.app.context.AppContext;
import pl.mewash.common.downloads.impl.DefaultDownloadService;

import java.util.function.BiConsumer;

public final class DownloadServiceProvider {

    public static DownloadService getDefaultDownloadService(AppContext appContext) {
        return new DefaultDownloadService(appContext);
    }

    public static DownloadService getDefaultDownloadServiceWithResourceLogger(AppContext appContext,
                                                                              BiConsumer<String, Object[]> logConsumer) {
        DefaultDownloadService defaultDownloadService = new DefaultDownloadService(appContext);
        defaultDownloadService.injectLogger(logConsumer);
        return defaultDownloadService;
    }
}
