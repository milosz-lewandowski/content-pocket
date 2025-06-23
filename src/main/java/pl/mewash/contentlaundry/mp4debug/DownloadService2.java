package pl.mewash.contentlaundry.mp4debug;


import pl.mewash.contentlaundry.models.general.AdvancedOptions;
import pl.mewash.contentlaundry.models.general.enums.Formats;
import pl.mewash.contentlaundry.utils.LoggerUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

public class DownloadService2 {

    private BiConsumer<String, Object[]> logConsumer;

    public void setLogConsumer(BiConsumer<String, Object[]> consumer) {
        this.logConsumer = consumer;
    }

    public void download(String url, Formats format, String basePath, AdvancedOptions advancedOptions) throws IOException, InterruptedException {
        appendLog("log.washing_and_drying", url, format.name());

        // FIXME: Step 2: Temp title file
        Path basePathDir = Paths.get(basePath);
        Path tempTitleFile = Files.createTempFile(basePathDir, "temp_title", ".txt");
        System.out.println("mp4 debug base path dir: " + basePathDir);
        System.out.println("mp4 debug temp file: " + tempTitleFile);

        ProcessBuilder builder = ProcessFactory2.buildProcessCommand(url, format, advancedOptions, tempTitleFile);
        builder.directory(basePathDir.toFile());
        Process process = builder.start();

        // FIXME: step 2.1 switch title reading form input stream to file
//        String title = getTitle(process);
//        printProcessLogs(process); // FIXME: Step 2.1 consume process output
        LoggerUtils.synchronizedLogProcessOutputToFile(process);
        int exitCode = process.waitFor();
        String title = Files.readString(tempTitleFile, StandardCharsets.UTF_8).trim();
        Files.deleteIfExists(tempTitleFile);


        if (exitCode != 0) {
            System.err.println("Error downloading: " + title + " as " + format.value);
            appendLog("log.failed_to_process", title, format.value);
        } else {
            if (title.isEmpty()) {
                System.out.println("No download title: " + title + "." + format.value + " -> file probably already downloaded");
                appendLog("log.already_saved", title, format.value);
            } else {
                System.out.println("Downloaded: " + title + " as " + format.value);
                appendLog("log.laundry_ready", title, format.value);
            }
        }
    }

//    private void printProcessLogs(Process process) throws IOException {
//        BufferedReader reader = new BufferedReader(
//                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            System.out.println(line);
//        }
//    }
//
//    private static String getTitle(Process process) throws IOException {
//        BufferedReader reader = new BufferedReader(
//                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
//        String line;
//        String title = null;
//
//        while ((line = reader.readLine()) != null) {
//            if (line.startsWith("[download] Destination: ")) {
//                String filename = line.substring("[download] Destination: ".length()).trim();
//                int dotIndex = filename.lastIndexOf('.');
//                title = (dotIndex > 0) ? filename.substring(0, dotIndex) : filename;
//            }
//        }
//        return title;
//    }

    private void appendLog(String key, Object... params) {
        if (logConsumer != null) {
            logConsumer.accept(key, params);
        }
    }
}
