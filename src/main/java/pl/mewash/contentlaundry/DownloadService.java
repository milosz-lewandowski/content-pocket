package pl.mewash.contentlaundry;

import pl.mewash.contentlaundry.models.AdvancedOptions;
import pl.mewash.contentlaundry.utils.Formats;
import pl.mewash.contentlaundry.utils.ProcessFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DownloadService {


    private ResourceBundle resources;

    public void setResources(ResourceBundle bundle) {
        this.resources = bundle;
    }
    private BiConsumer<String, Object[]> logConsumer;

    public void setLogConsumer(BiConsumer<String, Object[]> consumer) {
        this.logConsumer = consumer;
    }

    public void download(String url, Formats format, String basePath, AdvancedOptions advancedOptions) throws IOException, InterruptedException {

        appendLog("log.washing_and_drying", url, format.name());

        ProcessBuilder builder = ProcessFactory.buildProcessCommand(url, format, advancedOptions);

        builder.directory(new File(basePath));

        Process process = builder.start();
        String title = getTitle(process);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Error downloading: " + title + " as " + format.value);
            appendLog("log.failed_to_process", title, format.value);
        } else {
            if (title == null || title.isEmpty()) {
                System.out.println("No download title: " + title + "." + format.value + " -> file probably already downloaded");
                appendLog("log.already_saved", title, format.value);
            } else {
                System.out.println("Downloaded: " + title + " as " + format.value);
                appendLog("log.laundry_ready", title, format.value);
            }
        }
    }

    private static String getTitle(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line;
        String title = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("[download] Destination: ")) {
                String filename = line.substring("[download] Destination: ".length()).trim();
                int dotIndex = filename.lastIndexOf('.');
                title = (dotIndex > 0) ? filename.substring(0, dotIndex) : filename;
            }
        }
        return title;
    }

    private void appendLog(String key, Object... params) {
        if (logConsumer != null) {
            logConsumer.accept(key, params);
        }
    }
}