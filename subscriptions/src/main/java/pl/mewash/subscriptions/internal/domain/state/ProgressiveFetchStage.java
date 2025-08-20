package pl.mewash.subscriptions.internal.domain.state;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
public enum ProgressiveFetchStage {
    FIRST_FETCH("Fetch", false),
    FETCH_LATEST("Fetch Latest", false),
    FETCHING("Fetching", true),
    FETCH_OLDER("Check OlderRange", false),
    ALL_FETCHED("All Fetched", false),
    FETCH_ERROR("Fetch Error", true);

    @Getter private final String buttonTitle;
    @Getter private final boolean isDisabled;

    public String getButtonTitleWithOlderResolved(ProgressiveFetchRange fetchRange) {
        return this.equals(FETCH_OLDER)
            ? fetchRange.getButtonTitle()
            : this.buttonTitle;
    }

    public ProgressiveFetchRange getProgressiveRange(LocalDateTime oldestContentOrPrevFetchDateAfter) {
        if (!(this == FETCH_OLDER || this == ALL_FETCHED)) throw new IllegalStateException("Fetching in wrong stage");
        return ProgressiveFetchRange.getFirstWithTriggerPeriod(oldestContentOrPrevFetchDateAfter.toLocalDate());
    }


}
