package pl.mewash.subscriptions.internal.domain.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;

@AllArgsConstructor
public enum ProgressiveFetchRange {
    LAST_1_MONTH(Period.ofMonths(1), "Fetch 1 Month", Period.ofDays(0), Period.ofDays(21)),
    LAST_3_MONTHS(Period.ofMonths(3), "Fetch 3 Months", Period.ofDays(20), Period.ofMonths(2)),
    LAST_6_MONTHS(Period.ofMonths(6), "Fetch 6 Months", Period.ofMonths(2).minusDays(1), Period.ofMonths(4)),
    LAST_YEAR(Period.ofYears(1), "Fetch 1 Year", Period.ofMonths(4).minusDays(1), Period.ofMonths(9)),
    LAST_3_YEARS(Period.ofYears(3), "Fetch 3 Years", Period.ofMonths(9).minusDays(1), Period.ofYears(2)),
    LAST_10_YEARS(Period.ofYears(10), "Fetch 10 Years", Period.ofYears(2).minusDays(1), Period.ofYears(5)),
    LAST_25_YEARS(Period.ofYears(25), "Fetch All", Period.ofYears(5).minusDays(1), Period.ofYears(30)),
    ;

    private final Period dateRange;
    @Getter final String buttonTitle;
    private final Period triggerPeriodStart;
    private final Period triggerPeriodEnd;

    public LocalDateTime calculateDateAfter() {
        return LocalDateTime.now().minus(this.dateRange);
    }

    public static ProgressiveFetchRange getFirstWithTriggerPeriod(LocalDate oldestContentPublDate) {
        return Arrays.stream(values())
            .filter(range -> {
                boolean isBefore = oldestContentPublDate.isBefore(LocalDate.now().minus(range.triggerPeriodStart));
                boolean isAfter = oldestContentPublDate.isAfter(LocalDate.now().minus(range.triggerPeriodEnd));
                return isAfter && isBefore;
            })
            .findFirst()
            .orElse(LAST_25_YEARS);
    }
}
