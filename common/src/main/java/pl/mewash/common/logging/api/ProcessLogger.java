package pl.mewash.common.logging.api;

import java.util.List;

/**
 * Interface for managing process output logging in a non-blocking manner.
 *
 * <p>ProcessLogger instances are created by {@link FileLogger#getNewProcessLogger()}
 * and provide independent control over process output capture and logging.
 * All captured output is still flushed to the shared logging buffer while allowing
 * individual process management.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * ProcessLogger logger = fileLogger.createProcessLogger();
 * logger.startLogging(process);
 *
 * // ... do other work while logging happens in background ...
 *
 * List<String> output = logger.awaitCompletion(5000);
 * logger.shutdown();
 * }</pre>
 */
public interface ProcessLogger {

    /**
     * Start logging process output in a background thread.
     * This method returns immediately and doesn't block.
     *
     * @param process the process whose output should be logged
     */
    void startLogging(Process process);

    /**
     * Shutdown this process logger.
     * Stops reading from the process streams and returns collected output.
     *
     * @return list of all output lines collected from stdout
     */
    List<String> shutdownAndGetSnapshot();

    /**
     * Get the output collected so far without shutting down the logger.
     *
     * @return snapshot of currently collected output lines
     */
    List<String> getOutput();

    /**
     * Wait for the logger to finish reading all process output (process streams closed).
     * This method blocks until the process streams are closed or an interruption occurs.
     *
     * @return list of all output lines collected from stdout
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    List<String> awaitCompletion() throws InterruptedException;

    /**
     * Wait for the logger to finish with a timeout.
     * This method blocks until the process streams are closed, the timeout expires,
     * or an interruption occurs.
     *
     * @param timeoutMillis maximum time to wait in milliseconds
     * @return list of all output lines collected from stdout (may be incomplete if timeout occurred)
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    List<String> awaitCompletion(long timeoutMillis) throws InterruptedException;
}