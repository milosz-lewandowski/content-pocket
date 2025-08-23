package pl.mewash.common.spi.tabs;

import java.util.Optional;

public interface TabPlugin {

    int positionOrder();
    String id();
    String title();
    String fxmlPath();
    Optional<String> resBndlLocation();
}
