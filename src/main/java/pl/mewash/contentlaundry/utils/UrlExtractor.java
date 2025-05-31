package pl.mewash.contentlaundry.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UrlExtractor {
    private static final String SPLIT_REGEX = "[,;\\s\\n]+";

    // Utility
    public static List<String> toUrlList(String input) {
        return Arrays.stream(input.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(token -> token.startsWith("http"))
                .toList();
    }


    // Local execution
    private static final String localInput = "https://www.youtube.com/watch?v=c_5zutEqTWo | Bad Ass Hardtek \uD83D\uDD25 Powerful Frenchcore Mix #181 - YouTube\n" +
            "https://www.youtube.com/watch?v=T15mhulRZSc&t=2s | Atomic Opera \uD83C\uDFAD Powerful Uptempo Hardcore Mix #179 - YouTube\n" +
            "https://www.youtube.com/watch?v=HPz7OASlAMQ | Ardeur De La Vie | Uptempo Frenchcore Mix - YouTube\n";

    public static void main(String[] args) {
        System.out.println(toMultiUrlString(localInput));
    }

    public static String toMultiUrlString(String input) {
        return Arrays.stream(input.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(token -> token.startsWith("http"))
                .map(url -> "\"" + url + "\",")
                .collect(Collectors.joining("\n"));
    }
}
