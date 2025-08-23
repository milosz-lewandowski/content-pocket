package pl.mewash.batch.internals.models;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

@RequiredArgsConstructor
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
        return threadsOperator.applyAsInt(availableThreads);
    }

    @RequiredArgsConstructor
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
