module pl.mewash.common {
    requires static lombok;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;

    requires pl.mewash.commands;

    exports pl.mewash.common.app.binaries;
    exports pl.mewash.common.app.config;
    exports pl.mewash.common.app.context;
    exports pl.mewash.common.app.lifecycle;
    exports pl.mewash.common.app.settings;

    exports pl.mewash.common.downloads.api;
    exports pl.mewash.common.logging.api;
    exports pl.mewash.common.spi.tabs;
    exports pl.mewash.common.temporary;
}