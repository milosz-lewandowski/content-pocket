package pl.mewash.subscriptions.a_subscriptions.models.channel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ChannelFetchingStage {
    FIRST_FETCH("Fetch", false),
    FETCH_LATEST("Fetch Latest", false),
    FETCHING("Fetching", true),
    FETCH_OLDER("Check OlderRange", false),
    FETCH_ERROR("Fetch Error", true);

    private final String buttonTitle;
    private final boolean isDisabled;

    public String getButtonTitleWithOlderResolved(FetchOlderRange olderRange) {
        return this.equals(FETCH_OLDER)
            ? olderRange.getButtonTitle()
            : this.buttonTitle;
    }

    @AllArgsConstructor
    public enum FetchOlderRange {
        LAST_1_MONTH(Period.ofMonths(1), "Fetch 1 Month", Period.ofDays(0), Period.ofDays(21)),
        LAST_3_MONTHS(Period.ofMonths(3), "Fetch 3 Months", Period.ofDays(20), Period.ofMonths(2)),
        LAST_6_MONTHS(Period.ofMonths(6), "Fetch 6 Months", Period.ofMonths(2).minusDays(1), Period.ofMonths(4)),
        LAST_YEAR(Period.ofYears(1), "Fetch 1 Year", Period.ofMonths(4).minusDays(1), Period.ofMonths(9)),
        LAST_3_YEARS(Period.ofYears(3), "Fetch 3 Years", Period.ofMonths(9).minusDays(1), Period.ofYears(2)),
        LAST_10_YEARS(Period.ofYears(10), "Fetch 10 Years", Period.ofYears(2).minusDays(1), Period.ofYears(5)),
        LAST_25_YEARS(Period.ofYears(25), "Fetch All", Period.ofYears(5).minusDays(1), Period.ofYears(25)),
        ;

        private final Period dateRange;
        @Getter final String buttonTitle;
        private final Period triggerPeriodStart;
        private final Period triggerPeriodEnd;

        public LocalDateTime calculateDateAfter() {
            return LocalDateTime.now().minus(this.dateRange);
        }

        public static FetchOlderRange getFirstWithTriggerPeriodLongerThanCompared(LocalDate oldestContentPublishedDate) {
            return Arrays.stream(values())
                .filter(range -> {
                    boolean isBefore = oldestContentPublishedDate.isBefore(LocalDate.now().minus(range.triggerPeriodStart));
                    boolean isAfter = oldestContentPublishedDate.isAfter(LocalDate.now().minus(range.triggerPeriodEnd));
                    return isAfter && isBefore;
                })
                .findFirst()
                .orElseThrow();
        }
    }

    public FetchOlderRange getOlderRange(LocalDateTime oldestContentOrPreviousFetchDateAfter) {
        if (this != FETCH_OLDER) throw new IllegalStateException("Fetching in wrong stage");
        return FetchOlderRange.getFirstWithTriggerPeriodLongerThanCompared(oldestContentOrPreviousFetchDateAfter.toLocalDate());
    }


}
