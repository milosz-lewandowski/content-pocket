//package pl.mewash.contentlaundry.models.content;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import pl.mewash.contentlaundry.models.channel.enums.ChannelFetchingStage;
//
//import java.time.LocalDateTime;
//import java.util.EnumSet;
//
//@Getter
//@Builder
//@AllArgsConstructor
//public class FetchRangeContext {
//    private LocalDateTime lastFetchDate;
//    private LocalDateTime previousFetchOlderRangeDate;
//    private EnumSet<ChannelFetchingStage.FetchOlderRange> recentlyUsedOlderRanges = EnumSet.noneOf(ChannelFetchingStage.FetchOlderRange.class);
//
//    private LocalDateTime latestFetchedContentDate;
//    private LocalDateTime oldestFetchedContentDate;
//
////    private double contentDensityInTime; //for timeout based fetches
//
//    public static FetchRangeContext ofDateAfterFetch(ChannelFetchingStage.FetchOlderRange currentlyUsedOlderRange,
//                                                     LocalDateTime latestFetchedContentDate,
//                                                     LocalDateTime oldestFetchedContentDate) {
//        return FetchRangeContext.builder()
//                .lastFetchDate(LocalDateTime.now())
//                .recentlyUsedOlderRanges(EnumSet.of(currentlyUsedOlderRange))
//                .previousFetchOlderRangeDate(currentlyUsedOlderRange.calculateDateAfter())
//                .latestFetchedContentDate(latestFetchedContentDate)
//                .oldestFetchedContentDate(oldestFetchedContentDate)
//                .build();
//    }
//
//    public static FetchRangeContext ofTimeoutFetch(){
//
//    }
//}
