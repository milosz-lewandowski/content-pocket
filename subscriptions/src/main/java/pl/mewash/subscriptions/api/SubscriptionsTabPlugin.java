package pl.mewash.subscriptions.api;

import pl.mewash.common.spi.tabs.TabPlugin;

public final class SubscriptionsTabPlugin implements TabPlugin {
    @Override
    public int positionOrder() {
        return 1;
    }

    @Override
    public String id() {
        return "subscriptions";
    }

    @Override
    public String title() {
        return "Subscribed laundry";
    }

    @Override
    public String fxmlPath() {
        return "/pl/mewash/subscriptions/ui/subscriptions-view.fxml";
    }
}
