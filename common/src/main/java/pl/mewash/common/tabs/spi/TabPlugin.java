package pl.mewash.common.tabs.spi;

public interface TabPlugin {

    int positionOrder();
    String id();
    String title();
    String fxmlPath();
}
