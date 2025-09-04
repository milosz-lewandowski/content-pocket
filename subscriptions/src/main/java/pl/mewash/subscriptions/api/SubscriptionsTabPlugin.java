package pl.mewash.subscriptions.api;

import pl.mewash.common.spi.tabs.TabPlugin;

import java.util.Optional;

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
        return "Subscriptions pocket";
    }

    @Override
    public String fxmlPath() {
        return "/pl/mewash/subscriptions/ui/subscriptions-view.fxml";
    }

    @Override
    public Optional<String> resBundleLocation() {
        return Optional.empty();
    }
}
