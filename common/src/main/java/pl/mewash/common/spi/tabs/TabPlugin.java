package pl.mewash.common.spi.tabs;

public interface TabPlugin {

    int positionOrder();
    String id();
    String title();
    String fxmlPath();
}
