module pl.mewash.common {
    requires static lombok;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;

    requires pl.mewash.commands;

    exports pl.mewash.common;
    exports pl.mewash.common.tabs.spi;
}