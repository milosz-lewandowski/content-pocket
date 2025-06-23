package pl.mewash.contentlaundry.models.general.enums;

import lombok.RequiredArgsConstructor;

import java.util.function.IntUnaryOperator;

@RequiredArgsConstructor
public enum MultithreadingMode {
    SINGLE((availableThreads) -> 1),
    LOW((availableThreads) -> Math.max(2, availableThreads / 4)),
    MEDIUM((availableThreads) -> Math.max(3, availableThreads / 2)),
    MAXIMUM((availableThreads) -> Math.max(2, availableThreads / -1));

    private final IntUnaryOperator threadsOperator;
    private final static int reservedThreads = ReservedThreads.values().length;

    public int calculateThreads(){
        int totalPlatformThreads = Runtime.getRuntime().availableProcessors();
        int availableThreads = totalPlatformThreads - reservedThreads;

        return threadsOperator.applyAsInt(availableThreads);
    }

    private enum ReservedThreads {
        MainUiThread,
        ScheduledUiLoggerThread,
        LoopIteratorThread,
        JVM;
    }
}
