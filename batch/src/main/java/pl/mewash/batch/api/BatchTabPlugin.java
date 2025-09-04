package pl.mewash.batch.api;

import pl.mewash.common.spi.tabs.TabPlugin;

import java.util.Optional;

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
        return "Batch pocket";
    }

    @Override
    public String fxmlPath() {
        return "/pl/mewash/batch/ui/batch-view.fxml";
    }

    /**
     * Batch Tab uses its own messages.properties file
     * @return location of tab resource file
     */
    @Override
    public Optional<String> resBundleLocation() {
        return Optional.of( "pl.mewash.batch.i18n.messages");
    }
}
