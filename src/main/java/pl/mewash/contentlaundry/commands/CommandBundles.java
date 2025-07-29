package pl.mewash.contentlaundry.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum CommandBundles {
//    FORCE_H264_RECODING(false, List.of(
//            "--recode-video", "mp4",
//            "--audio-format", "aac"
////            "--postprocessor-args", "-c:v libx264 -preset medium -crf 23 -c:a aac -b:a 256k"
//    )),
    EXTRACT_BEST_AUDIO(false, List.of(
            "--merge-output-format", "mp4",
            "--audio-format", "aac",
            "--audio-quality", "0"
    )),
    EMBED_FILE_METADATA(false, List.of(
            "--embed-thumbnail",
            "--add-metadata"
    )),
    ADD_CONTENT_METADATA_FILES(false, List.of(
            "--write-description",
            "--write-info-json"
    )),
    CHECK_CHANNEL_NAME(false, List.of(
            "--skip-download",
            "--playlist-end", "1"
            , "--quiet" // FIXME: check why disabling quiet causes print to ui
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
    BEST_AUDIO_CODEC(false, List.of(

    ));

    final boolean withParam;
    final List<String> params;

    public void appendAtEndOf(List<String> currentParamList) {
        currentParamList.addAll(this.getParams());
    }
}