package pl.mewash.contentlaundry.utils;

import pl.mewash.contentlaundry.subscriptions.ConfigPaths;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScheduledFileLogger {

    // local thread buffer for related logs grouping
    private static final List<ThreadBuffer> localThreadBuffersList = new CopyOnWriteArrayList<>();
    private static final ThreadLocal<ThreadBuffer> threadBuffer = ThreadLocal.withInitial(ThreadBuffer::new);

    // shared buffer to scheduled file logging
    private static final ConcurrentLinkedQueue<String> sharedLogBuffer = new ConcurrentLinkedQueue<>();
    private static final Object logFileLock = new Object();

    // logger time formatter
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // scheduled writer thread setup
    private static final AtomicBoolean writerStarted = new AtomicBoolean(false);
    private static final Duration WRITE_INTERVAL = Duration.ofSeconds(5);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "scheduled-file-logger-thread");
        t.setDaemon(true);
        return t;
    });

    static {
        setupScheduledLogFileWriter();
    }

    private static void setupScheduledLogFileWriter() {
        if (writerStarted.getAndSet(true)) return;

        scheduler.scheduleAtFixedRate(
                ScheduledFileLogger::flushLogsToFile,
                WRITE_INTERVAL.toMillis(),
                WRITE_INTERVAL.toMillis(),
                TimeUnit.MILLISECONDS
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduler.shutdown();
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            localThreadBuffersList.forEach(ThreadBuffer::flush);

            flushLogsToFile();
        }));
    }

    public static void appendSingleLine(String message) {
        appendStringList(List.of(message));
    }

    public static void appendStringList(List<String> list) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        String threadName = shortThreadName();
        for (String l : list) {
            sharedLogBuffer.add(time + " [" + threadName + "]: " + l);
        }
    }

    public static void consumeAndLogProcessOutputToFile(Process process) {
        ThreadBuffer localThreadBuffer = threadBuffer.get();

        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String threadName = shortThreadName();
            String line;


            while ((line = stdOut.readLine()) != null) {
                if (!isYtDlpProgressMessageLine(line)) {
                    String time = LocalDateTime.now().format(TIME_FORMAT);
                    localThreadBuffer.add("[OUT] " + time + " [" + threadName + "]: " + line.trim());
                }
            }

            while ((line = stdErr.readLine()) != null) {
                if (!isYtDlpProgressMessageLine(line)) {
                    String time = LocalDateTime.now().format(TIME_FORMAT);
                    localThreadBuffer.add("[ERR] " + time + " [" + threadName + "]: " + line.trim());
                }
            }

        } catch (IOException e) {
            System.err.println("Error logging process to file output: " + e.getMessage());
        } finally {
            localThreadBuffer.flush();
            localThreadBuffersList.remove(localThreadBuffer);
            threadBuffer.remove();
        }
    }

    private static void flushLogsToFile() {
        try {
            Path logDir = ConfigPaths.getLogsDir();
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Path logFile = logDir.resolve(date + ".log");

            synchronized (logFileLock) {
                try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    String line;
                    while ((line = sharedLogBuffer.poll()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Error writing log lines: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error accessing to logging file: " + e.getMessage());
        }
    }

    private static String shortThreadName() {
        Thread t = Thread.currentThread();
        String name = t.getName();

        if (t.isVirtual()) {
            return name;
        } else if (name.startsWith("pool")) {
            return "PoolT-" + name.replaceAll(".*-(\\d+)$", "$1");
        } else {
            return name.length() > 15 ? name.substring(0, 15) : name;
        }
    }

    private static boolean isYtDlpProgressMessageLine(String line) {
        // yt-dlp / ffmpeg progress lines usually include '% of'  or 'frame=' info
        return line.contains("% of") || line.matches(".*\\d+\\.\\d+%.*") || line.contains("frame=");
    }


    private static class ThreadBuffer {
        private final List<String> buffer = new ArrayList<>();
        private long lastFlush = System.currentTimeMillis();

        ThreadBuffer() {
            localThreadBuffersList.add(this);
        }

        void add(String message) {
            buffer.add(message);
            long now = System.currentTimeMillis();
            if (now - lastFlush >= 4800 || buffer.size() >= 50) {
                flush();
                lastFlush = now;
            }
        }

        void flush() {
            if (!buffer.isEmpty()) {
                sharedLogBuffer.addAll(buffer);
                buffer.clear();
            }
        }
    }
}
