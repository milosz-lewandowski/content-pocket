package pl.mewash.batch.internals.models;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

@AllArgsConstructor
public enum MultithreadingMode {
    SINGLE((availableThreads) -> 1),
    LOW((availableThreads) -> Math.max(2, availableThreads / 4)),
    MEDIUM((availableThreads) -> Math.max(3, availableThreads / 2)),
    MAXIMUM((availableThreads) -> Math.max(4, availableThreads -1));

    private final IntUnaryOperator threadsOperator;
    private final static int reservedThreads = ReservedThreads.getCurrentlyReservedThreadsCount();

    public int calculateThreads(){
        int totalPlatformThreads = Runtime.getRuntime().availableProcessors();
        int availableThreads = totalPlatformThreads - reservedThreads;
        int calculatedThreads = threadsOperator.applyAsInt(availableThreads);
        System.out.println("calculated threads: " + calculatedThreads);
        return calculatedThreads;
    }

    @AllArgsConstructor
    private enum ReservedThreads {
        MainUiThread(true),                 // required - for smooth ui experience
        ScheduledUiLoggerThread(false),     // not required - rare write intervals
        JobDispatcherThread(false),         // not required - rare condition checks
        JVM(true);                          // required - just for throttling safety

        private final boolean needReservation;

        private static int getCurrentlyReservedThreadsCount() {
            return (int) Arrays.stream(ReservedThreads.values())
                .filter(thread -> thread.needReservation)
                .count();
        }
    }
}
