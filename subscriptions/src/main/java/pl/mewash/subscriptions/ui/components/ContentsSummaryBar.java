package pl.mewash.subscriptions.ui.components;

import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Stream;

public class ContentsSummaryBar {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Label channelName = new Label("-");
    private final Label totalContents = new Label("0");

    private final Label lastFetchDate = new Label("-");
    private final Label fetchedSinceDate = new Label("-");

    private final Label areLatestFetched = new Label("✅");
    private final Label isFetchedOldest = new Label("❌");

    private final Label savedAudios = new Label("0");
    private final Label savedVideos = new Label("0");

    public GridPane buildGridPane(Params params) {
        setLabelsValues(params);

        // --- main grid ---
        GridPane main = new GridPane();
        main.setHgap(20);
        main.setVgap(2);
        main.setStyle("-fx-padding: 6; -fx-background-color: -fx-background;");

        ColumnConstraints C0 = new ColumnConstraints();
        C0.setHgrow(Priority.ALWAYS);

        ColumnConstraints C1 = new ColumnConstraints();
        C1.setHalignment(HPos.RIGHT);

        main.getColumnConstraints().addAll(C0, C1);

        // --- left grid ---
        GridPane l = new GridPane();
        l.setHgap(10);
        l.setVgap(2);

        l.add(new Label("Fetched Latest"),3, 0);    l.add(areLatestFetched,4, 0);
        l.add(new Label("Fetched Oldest"),3, 1);    l.add(isFetchedOldest, 4, 1);

        l.add(spacerL(), 2, 0);                        l.add(spacerL(), 2, 1);

        l.add(new Label("Last Fetch:"),    0, 0);   l.add(lastFetchDate,   1, 0);
        l.add(new Label("Fetched Since:"), 0, 1);   l.add(fetchedSinceDate,1, 1);

        GridPane.setHalignment(lastFetchDate, HPos.RIGHT);
        GridPane.setHalignment(fetchedSinceDate, HPos.RIGHT);

        // --- right grid ---
        GridPane r = new GridPane();
        r.setHgap(10);
        r.setVgap(2);

        ColumnConstraints R0 = new ColumnConstraints();
        R0.setHgrow(Priority.ALWAYS);
        ColumnConstraints R1 = new ColumnConstraints();
        R1.setHalignment(HPos.RIGHT);
        r.getColumnConstraints().addAll(R0, R1);

        r.add(savedAudios,    0, 0);                r.add(new Label("Audios Saved"), 1, 0);
        r.add(savedVideos,    0, 1);                r.add(new Label("Videos Saved"), 1, 1);

        r.add(spacerL(), 2, 0);                     r.add(spacerL(), 2, 1);

        r.add(channelName, 3, 0);                   GridPane.setColumnSpan(channelName, 2);
        r.add(new Label("Contents:"),     3, 1); r.add(totalContents,  4, 1);

        GridPane.setHalignment(channelName, HPos.RIGHT);
        GridPane.setHalignment(totalContents, HPos.RIGHT);

        main.add(l, 0, 0, 1, 2);
        main.add(r, 1, 0, 1, 2);

        Stream.of(channelName, savedAudios, totalContents, savedVideos)
            .forEach(label -> label.setStyle("-fx-font-weight: bold;"));

        return main;
    }

    private Label spacerL() {
        return new Label("|");
    }

    private void setLabelsValues(Params params) {
        channelName.setText(params.channelName);
        totalContents.setText(String.valueOf(params.contentsCount));

        lastFetchDate.setText(formatLastFetchDateMessage(params.lastFetchDate));
        areLatestFetched.setText(params.fetchedLatest ? "✅" : "❌");

        fetchedSinceDate.setText(params.fetchedSince.format(DATE_FORMAT));
        isFetchedOldest.setText(params.fetchedOldest ? "✅" : "❌");

        savedAudios.setText(String.valueOf(params.savedAudios));
        savedVideos.setText(String.valueOf(params.savedVideos));
    }

    private String formatLastFetchDateMessage(LocalDateTime lastFetch) {
        LocalDate fetchDay = lastFetch.toLocalDate();
        LocalDate today = LocalDate.now();

        if (fetchDay.isEqual(today))
            return "Today " + TIME_FORMAT.format(lastFetch);
        if (today.minusDays(1).isEqual(fetchDay))
            return "Yesterday " + TIME_FORMAT.format(lastFetch);
        if (fetchDay.isBefore(today.minusDays(1)) && fetchDay.isAfter(today.minusDays(7))) {
            String dayOfWeek = fetchDay.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
            return dayOfWeek + " " + TIME_FORMAT.format(lastFetch);
        }
        return fetchDay.format(DATE_FORMAT);
    }

    @Builder
    @AllArgsConstructor
    public static final class Params {
        private final String channelName;
        private final int contentsCount;
        private final LocalDateTime lastFetchDate;
        private final boolean fetchedLatest;
        private final LocalDateTime fetchedSince;
        private final boolean fetchedOldest;
        private final int savedAudios;
        private final int savedVideos;
    }
}
