package pl.mewash.batch;

import lombok.RequiredArgsConstructor;

import java.util.function.IntUnaryOperator;

@RequiredArgsConstructor
public enum MultithreadingMode {
    SINGLE((availableThreads) -> 1),
    LOW((availableThreads) -> Math.max(2, availableThreads / 4)),
    MEDIUM((availableThreads) -> Math.max(3, availableThreads / 2)),
    MAXIMUM((availableThreads) -> Math.max(4, availableThreads -1));

    private final IntUnaryOperator threadsOperator;
    private final static int reservedThreads = ReservedThreads.values().length;

    public int calculateThreads(){
        int totalPlatformThreads = Runtime.getRuntime().availableProcessors();
        int availableThreads = totalPlatformThreads - reservedThreads;
        int calculatedThreads = threadsOperator.applyAsInt(availableThreads);
        System.out.println("calculated threads: " + calculatedThreads);
        return calculatedThreads;
    }

    private enum ReservedThreads {
        MainUiThread,
        ScheduledUiLoggerThread,
        LoopIteratorThread,
        JVM;
    }
}
