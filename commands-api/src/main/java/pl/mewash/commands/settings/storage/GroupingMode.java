package pl.mewash.commands.settings.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum GroupingMode {
    GROUP_BY_FORMAT("By Codec - contents grouped in dirs by codecs"),
    GROUP_BY_CONTENT("By Content - each content in all codecs grouped together"),
    NO_GROUPING("No Grouping - all contents and codecs together"),
    ;

    @Getter private final String description;
}
