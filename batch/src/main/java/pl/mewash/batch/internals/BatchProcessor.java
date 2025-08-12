package pl.mewash.batch.internals;

import pl.mewash.batch.internals.models.BatchJobParams;
import pl.mewash.batch.internals.models.BatchProcessingState;
import pl.mewash.batch.internals.models.MultithreadingMode;
import pl.mewash.commands.settings.formats.DownloadOption;
import pl.mewash.common.DownloadService;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BatchProcessor {

    private final DownloadService downloadService;
    private BatchJobParams currentJobParams;
    private JobDispatcher jobDispatcher;
    private ThreadPoolExecutor workersPool;

    private final Consumer<String> uiLogger;
    private final Consumer<String> errorLogger;

    private final AtomicReference<BatchProcessingState> processingState =
        new AtomicReference<>(BatchProcessingState.NOT_RUNNING);

    // inject button update action
    private volatile Consumer<BatchProcessingState> updateLaundryButtonListener;
    public void injectUpdateButtonAction(Consumer<BatchProcessingState> updateButtonEvent) {
        assert updateButtonEvent != null;
        this.updateLaundryButtonListener = updateButtonEvent;
    }

    private void updateProcessingState(BatchProcessingState state) {
        processingState.set(state);
        updateLaundryButtonListener.accept(state);
    }

    public BatchProcessor(Consumer<String> uiLogger, Consumer<String> errorLogger, DownloadService downloadService) {
        this.uiLogger = uiLogger;
        this.errorLogger = errorLogger;
        this.downloadService = downloadService;
    }

    public BatchProcessingState getProcessingState() {
        return processingState.get();
    }

    public void gracefulShutdownAsync() {
        CompletableFuture.runAsync(() -> {
            updateProcessingState(BatchProcessingState.IN_GRACEFUL_SHUTDOWN);
            jobDispatcher.stop();
            if (workersPool == null) return;
            if (!workersPool.isShutdown()) workersPool.shutdown();
            int remainingTasks = jobDispatcher.getRemainingQueueSize();
            uiLogger.accept("\n--- Batch service shutting down gracefully. Completing in progress tasks and" +
                " rejecting remaining " + remainingTasks + " tasks. ---\n");

            try {
                if (!workersPool.awaitTermination(120, TimeUnit.SECONDS)) {
                    uiLogger.accept("--- Graceful shutdown timed out. Forcing shutdown.--- ");
                    updateProcessingState(BatchProcessingState.IN_FORCED_SHUTDOWN);
                    workersPool.shutdownNow();
                } else {
                    if (processingState.get() == BatchProcessingState.IN_GRACEFUL_SHUTDOWN) {
                        uiLogger.accept("\n--- Graceful shutdown completed. ---\n");
                    } else if (processingState.get() == BatchProcessingState.IN_FORCED_SHUTDOWN) {
                        uiLogger.accept("\n--- Forced shutdown completed. ---\n");
                    } else {
                        uiLogger.accept("\n--- Shutdown completed in incorrect state ---\n");
                    }
                    updateProcessingState(BatchProcessingState.NOT_RUNNING);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workersPool.shutdownNow();
                uiLogger.accept("--- Interrupted during graceful shutdown. Forcing shutdown. ---");
                updateProcessingState(BatchProcessingState.NOT_RUNNING);
            }
        });
    }

    public void forceShutdown() {
        jobDispatcher.stop();
        if (workersPool == null) return;
        if (!workersPool.isTerminated()) workersPool.shutdownNow();
        updateProcessingState(BatchProcessingState.IN_FORCED_SHUTDOWN);
        uiLogger.accept("\n--- Forcing shutdown on demand, aborting in progress tasks. ---\n" +
            "--- ! this may leave temporary and partial files ! ---\n");
    }

    public void processBatchLaundry(BatchJobParams jobParams) {
        updateProcessingState(BatchProcessingState.PROCESSING);

        currentJobParams = jobParams;
        workersPool = setupAndGetNoQueuedExecutor(jobParams);

        List<DownloadJob> jobs = currentJobParams.getUrlList().stream()
            .flatMap(url -> jobParams.getSelectedDownloadOptions().stream()
                .map(dOption -> new BatchProcessor.DownloadJob(url, dOption, jobParams)))
            .toList();

        jobDispatcher = new JobDispatcher(jobs, workersPool);
        jobDispatcher.enterDispatching();

        if (getProcessingState() != BatchProcessingState.IN_GRACEFUL_SHUTDOWN
            && getProcessingState() != BatchProcessingState.NOT_RUNNING) {
            uiLogger.accept("All downloads finished!");
            updateProcessingState(BatchProcessingState.NOT_RUNNING);
        }
    }

    class JobDispatcher {
        private final Queue<DownloadJob> jobQueue = new ConcurrentLinkedQueue<>();
        private final ThreadPoolExecutor workersPool;
        private volatile DispatchingStage dispatchingStage = DispatchingStage.COMPLETED_OR_ABORTED;

        JobDispatcher(List<DownloadJob> jobs, ThreadPoolExecutor workersPool) {
            this.workersPool = workersPool;
            this.jobQueue.addAll(jobs);
        }

        Predicate<ThreadPoolExecutor> anyWorkerAvailable = pool ->
            pool.getActiveCount() < pool.getMaximumPoolSize();

        enum DispatchingStage {
            DISPATCHING_QUEUE,
            AWAITING_COMPLETION,
            COMPLETED_OR_ABORTED,
        }

        void enterDispatching() {
            dispatchingStage = DispatchingStage.DISPATCHING_QUEUE;

            // 3. when completed or stop was requested: exit
            while (dispatchingStage != DispatchingStage.COMPLETED_OR_ABORTED) {
                switch (dispatchingStage) {

                    // 2. when queue is empty: await in progress tasks completion
                    case AWAITING_COMPLETION -> {
                        // 2.2. when all are completed: exit
                        if (currentJobParams.checkAllDownloadsCompleted()) {
                            dispatchingStage = DispatchingStage.COMPLETED_OR_ABORTED;
                        // 2.1. when tasks in progress: wait 200ms
                        } else sleepQuietly(200);
                    }
                    // 1. when queue not empty: dispatch
                    case DISPATCHING_QUEUE -> {

                        // 1.3. when all tasks are dispatched: change state
                        if (jobQueue.isEmpty()) {
                            dispatchingStage = DispatchingStage.AWAITING_COMPLETION;

                        // 1.1. when any worker available: attempt to submit job
                        } else if (anyWorkerAvailable.test(workersPool)) {
                            DownloadJob job = jobQueue.poll();
                            if (job != null) {

                                // 1.1.1. when queued job exists: submit and continue
                                try {
                                    workersPool.submit(job);

                                // 1.1.2. when rejected: requeue and wait 50ms
                                } catch (RejectedExecutionException ignored) {
                                    jobQueue.offer(job);
                                    sleepQuietly(50);
                                }
                            }

                        // 1.2. when all workers are busy: wait 100ms
                        } else {
                            sleepQuietly(100);
                        }
                    }
                }
            }
            workersPool.shutdown();
        }

        int getRemainingQueueSize() {
            return jobQueue.size();
        }

        void stop() {
            dispatchingStage = DispatchingStage.COMPLETED_OR_ABORTED;
        }

        private void sleepQuietly(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                dispatchingStage = DispatchingStage.COMPLETED_OR_ABORTED;
            }
        }
    }

    class DownloadJob implements Runnable {
        private final BatchJobParams params;
        private final String url;
        private final DownloadOption downloadOption;

        DownloadJob(String url, DownloadOption downloadOption, BatchJobParams params) {
            this.params = params;
            this.url = url;
            this.downloadOption = downloadOption;
        }

        public void run() {
            try {
                downloadService.downloadWithSettings(url, downloadOption, params.getBasePath(), params.getStorageOptions());
                params.getCompletedCount().incrementAndGet();
            } catch (Exception e) {
                String failureMessage = String.format(" Failed to download [%s] as [%s]%n", url, downloadOption);
                uiLogger.accept(failureMessage);
                errorLogger.accept(failureMessage + e.getMessage());
                params.getFailedCount().incrementAndGet();
            }
        }
    }

    private ThreadPoolExecutor setupAndGetNoQueuedExecutor(BatchJobParams params) {
        MultithreadingMode mode = params.getMultithreadingMode();
        int calculatedAvailableThreads = mode.calculateThreads();
        int fixedThreads = Math.min(params.calculateTotalDownloads(), calculatedAvailableThreads);
        String parallelMessage = mode == MultithreadingMode.SINGLE
            ? "Single thread: sequential downloads"
            : "Parallel mode: " + mode.name().toLowerCase() + ", worker threads: " + fixedThreads;
        uiLogger.accept(parallelMessage);

        return new ThreadPoolExecutor(
            fixedThreads, fixedThreads,
            0L, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
