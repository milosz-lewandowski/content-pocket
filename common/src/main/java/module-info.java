module pl.mewash.common {
    requires static lombok; // To removal in stable version

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    requires transitive pl.mewash.commands;

    exports pl.mewash.common.app.binaries;
    exports pl.mewash.common.app.config;
    exports pl.mewash.common.app.context;
    exports pl.mewash.common.app.lifecycle;
    exports pl.mewash.common.app.settings;

    exports pl.mewash.common.downloads.api;
    exports pl.mewash.common.logging.api;
    exports pl.mewash.common.spi.tabs;

    // For removal after some time if no problems appear
    exports pl.mewash.common.temporary;
}