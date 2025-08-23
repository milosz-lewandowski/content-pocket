package pl.mewash.common.spi.tabs;

import java.util.Optional;

/**
 * Defines contract and  properties required for discovery and loading view as plugin tab by ContentLaundryApp
 * via {@link java.util.ServiceLoader}
 */
public interface TabPlugin {

    /**
     * Used for sorting tabs and placing them in main ui tab pane.
     * @return a positive integer higher than 0
     */
    int positionOrder();

    /**
     * Unique identifier used for tabs distinction and identification.
     * Using module name is recommended
     * @return unique tab id
     */
    String id();

    /**
     * Title displayed as name of tab in UI tab pane
     * @return the tab title
     */
    String title();

    /**
     * Classpath relative path to the main fxml view for given tab
     * @return the String representing path to the fxml resource
     */
    String fxmlPath();

    /**
     * Base name for resource bundle properties file used by given tab.
     * If tab does This is not mandatory, if module does not use resource file than should return empty Optional
     * @return {@link Optional} with dot separated location of resource String
     *      or {@link Optional#empty} if tab does not use resources
     */
    Optional<String> resBundleLocation();
}
