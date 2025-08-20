package pl.mewash.common.logging.impl;

import pl.mewash.common.app.config.ConfigPaths;
import pl.mewash.common.logging.api.FileLogger;

import java.io.*;
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

public class ScheduledFileLogger implements FileLogger {

    // constants
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Duration WRITE_INTERVAL = Duration.ofSeconds(5);

    // singleton instance
    private static final ScheduledFileLogger INSTANCE = new ScheduledFileLogger();
    public static ScheduledFileLogger getInstance() {
        return INSTANCE;
    }

    // local thread buffer for related logs grouping
    private final List<ThreadBuffer> localThreadBuffersList = new CopyOnWriteArrayList<>();
    private final ThreadLocal<ThreadBuffer> threadBuffer = ThreadLocal.withInitial(() -> new ThreadBuffer(this));

    // shared buffer to scheduled file logging
    private final ConcurrentLinkedQueue<String> sharedLogBuffer = new ConcurrentLinkedQueue<>();
    private final Object logFileLock = new Object();

    // scheduled writer thread setup
    private final AtomicBoolean writerStarted = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "scheduled-file-logger-thread");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFileLogger() {
        setupScheduledLogFileWriter();
    }

    @Override
    public void appendSingleLine(String message) {
        appendMultiLineStringList(List.of(message));
    }

    @Override
    public void appendMultiLineStringList(List<String> list) {
        String time = LocalDateTime.now().format(TIME_FORMAT);
        String threadName = shortThreadName();
        for (String l : list) {
            sharedLogBuffer.add(time + " [" + threadName + "]: " + l);
        }
    }

    @Override
    public void logErrStackTrace(Throwable e, boolean serr) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        appendSingleLine(sw.toString());
        if (serr) System.err.println(sw);
    }

    @Override
    public void logErrWithMessage(String desc, Throwable e, boolean serr) {
        String message = desc + ": " + e.getMessage();
        appendSingleLine(message);
        if (serr) System.err.println(message);
    }

    @Override
    public void consumeAndLogProcessOutputToFile(Process process) {
        logProcessOutputToFile(process);
    }

    @Override
    public List<String> getProcessOutputAndLogToFile(Process process) {
        return logProcessOutputToFile(process);
    }


    private List<String> logProcessOutputToFile(Process process) {
        ThreadBuffer localThreadBuffer = threadBuffer.get();

        List<String> outputList = new ArrayList<>();

        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

            String threadName = shortThreadName();
            String line;

            while ((line = stdOut.readLine()) != null) {
                if (!isYtDlpProgressMessageLine(line)) {
                    outputList.add(line);
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
        return outputList;
    }

    private void setupScheduledLogFileWriter() {
        if (writerStarted.getAndSet(true)) return;

        scheduler.scheduleAtFixedRate(
            this::flushLogsToFile,
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

    private void flushLogsToFile() {
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
        private final ScheduledFileLogger owner;

        ThreadBuffer(ScheduledFileLogger owner) {
            this.owner = owner;
            owner.localThreadBuffersList.add(this);
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
                owner.sharedLogBuffer.addAll(buffer);
                buffer.clear();
            }
        }
    }
}
