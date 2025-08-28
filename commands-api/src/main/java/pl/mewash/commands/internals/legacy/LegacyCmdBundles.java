package pl.mewash.commands.internals.legacy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public enum LegacyCmdBundles {
    EXTRACT_BEST_AUDIO(false, List.of(
        "--merge-output-format", "mp4",
        "--audio-format", "aac",
        "--audio-quality", "0"
    )),
    EMBED_FILE_METADATA(false, List.of(
        "--embed-thumbnail",
        "--add-metadata"
    )),
    ADD_CONTENT_DESCRIPTION_FILES(false, List.of(
        "--write-description"
    )),
    ADD_CONTENT_METADATA_FILES(false, List.of(
        "--write-description",
        "--write-info-json"
    )),
    CHECK_CHANNEL_NAME(false, List.of(
        "--skip-download",
        "--playlist-end", "1",
        "--quiet" // FIXME: check why disabling quiet causes print to ui
    )),
    FETCH_CHANNEL_CONTENT(false, List.of(
        "--skip-download",
        "--match-filter", "!is_live",
        "--playlist-end", "7000", // Safety for infinite dangling process
        "--break-on-reject"
    )),
    ONLY_AUDIO_BEST_QUALITY(false, List.of( // TODO: make parametrized
        "--extract-audio",
        "--audio-quality", "0"
    )),
    ;

    final boolean withParam;
    final List<String> params;

    public void appendAtEndOf(List<String> currentParamList) {
        currentParamList.addAll(this.getParams());
    }
}