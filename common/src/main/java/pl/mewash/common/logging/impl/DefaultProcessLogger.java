package pl.mewash.common.logging.impl;

import pl.mewash.common.logging.api.ProcessLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultProcessLogger implements ProcessLogger {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private final ConcurrentLinkedQueue<String> sharedLogBuffer;
    private final String threadName;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final List<String> outputList = new ArrayList<>();
    private Thread readerThread;
    
    DefaultProcessLogger(ConcurrentLinkedQueue<String> sharedLogBuffer, String threadName) {
        this.sharedLogBuffer = sharedLogBuffer;
        this.threadName = threadName;
    }

    public void startLogging(Process process) {
        readerThread = Thread.ofVirtual().name("process-logger-" + threadName).start(() -> {
            try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                
                String line;
                while (running.get() && (line = stdOut.readLine()) != null) {
                    if (!isYtDlpProgressMessageLine(line)) {
                        outputList.add(line);
                        String time = LocalDateTime.now().format(TIME_FORMAT);
                        sharedLogBuffer.add("[OUT] " + time + " [" + threadName + "]: " + line.trim());
                    }
                }
                
                while (running.get() && (line = stdErr.readLine()) != null) {
                    if (!isYtDlpProgressMessageLine(line)) {
                        String time = LocalDateTime.now().format(TIME_FORMAT);
                        sharedLogBuffer.add("[ERR] " + time + " [" + threadName + "]: " + line.trim());
                    }
                }
                
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("Error logging process output: " + e.getMessage());
                }
            }
        });
    }

    public List<String> shutdownAndGetSnapshot() {
        running.set(false);
        if (readerThread != null) {
            readerThread.interrupt();
            try {
                readerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return new ArrayList<>(outputList);
    }

    public List<String> getOutput() {
        return new ArrayList<>(outputList);
    }

    public List<String> awaitCompletion() throws InterruptedException {
        if (readerThread != null) {
            readerThread.join();
        }
        return new ArrayList<>(outputList);
    }

    public List<String> awaitCompletion(long timeoutMillis) throws InterruptedException {
        if (readerThread != null) {
            readerThread.join(timeoutMillis);
        }
        return new ArrayList<>(outputList);
    }
    
    private static boolean isYtDlpProgressMessageLine(String line) {
        return line.contains("% of") || line.matches(".*\\d+\\.\\d+%.*") || line.contains("frame=");
    }
}
