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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LoggerUtils {

    private static final Object logFileLock = new Object();

    public static void synchronizedAppendSingleLine(String message) {
        try {
            Path logDir = ConfigPaths.getLogsDir();
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Path logFile = logDir.resolve(date + ".log");

            synchronized (logFileLock) {
                try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        writer.write(message);
                        writer.newLine();
                }
            }

        } catch (IOException e) {
            System.err.println("Error accessing to logging file: " + e.getMessage());
        }
    }

    public static void synchronizedAppendStringList(List<String> list) {
        try {
            Path logDir = ConfigPaths.getLogsDir();
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Path logFile = logDir.resolve(date + ".log");

            synchronized (logFileLock) {
                try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    for (String l :list) {
                        writer.write(l);
                        writer.newLine();
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error accessing to logging file: " + e.getMessage());
        }
    }

    public static void synchronizedConsumeAndLogProcessOutputToFile(Process process) {

        try {
            Path logDir = ConfigPaths.getLogsDir();
            String date = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Path logFile = logDir.resolve(date + ".log");



            try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                List<String> bufferedLines = new ArrayList<>();

                while ((line = stdOut.readLine()) != null) {
                    if (!isYtDlpProgressMessageLine(line)) {
                        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
                        bufferedLines.add("[OUT] " + time + " " + line);
                    }
                }

                while ((line = stdErr.readLine()) != null) {
                    String time = LocalDateTime.now().format(DateTimeFormatter.ISO_TIME);
                    if (!isYtDlpProgressMessageLine(line)) {
                        bufferedLines.add("[ERR] " + time + " " + line);
                    }
                }

                synchronized (logFileLock) {
                    try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                        for (String l :bufferedLines) {
                            writer.write(l);
                            writer.newLine();
                        }
                    }
                }

            } catch (IOException e) {
                System.err.println("Error logging process to file output: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Error accessing to logging file: " + e.getMessage());
        }
    }

    private static boolean isYtDlpProgressMessageLine(String line) {
        // yt-dlp / ffmpeg progress lines usually include '% of'  or 'frame=' info
        return line.contains("% of") || line.matches(".*\\d+\\.\\d+%.*") || line.contains("frame=");
    }
}
