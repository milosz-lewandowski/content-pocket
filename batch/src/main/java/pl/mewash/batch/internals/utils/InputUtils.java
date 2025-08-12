package pl.mewash.batch.internals.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InputUtils {
    private static final String SPLIT_REGEX = "[,;\\s\\n]+";

    public static List<String> toUrlList(String input) {
        return Arrays.stream(input.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(token -> token.startsWith("http"))
                .toList();
    }

    public static int getDetectedDuplicatesCount(List<String> urls) {
        return urls.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .values().stream()
                .filter(count -> count > 1)
                .mapToInt(count -> count.intValue() - 1)
                .sum();
    }

    public static List<String> removeDuplicates(List<String> urls) {
        return urls.stream()
                .distinct()
                .toList();
    }
}
