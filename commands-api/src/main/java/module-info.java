module pl.mewash.commands {
    requires static lombok;

    exports pl.mewash.commands.api.processes;
    exports pl.mewash.commands.api.entries;

    exports pl.mewash.commands.settings.response;
    exports pl.mewash.commands.settings.storage;
    exports pl.mewash.commands.settings.formats;
    exports pl.mewash.commands.settings.cmd;
}