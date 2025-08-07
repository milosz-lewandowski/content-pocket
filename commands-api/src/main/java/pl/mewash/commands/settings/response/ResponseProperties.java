package pl.mewash.commands.settings.response;

import java.util.Map;

public sealed interface ResponseProperties permits ChannelProperties, ContentProperties {

    String getPattern();
}
