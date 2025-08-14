package pl.mewash.batch.api;

import pl.mewash.common.spi.tabs.TabPlugin;

public final class BatchTabPlugin implements TabPlugin {

    @Override
    public int positionOrder() {
        return 2;
    }

    @Override
    public String id() {
        return "batch";
    }

    @Override
    public String title() {
        return "Batch laundry";
    }

    @Override
    public String fxmlPath() {
        return "/pl/mewash/batch/ui/batch-view.fxml";
    }
}
