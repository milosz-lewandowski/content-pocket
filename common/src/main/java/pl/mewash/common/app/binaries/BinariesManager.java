package pl.mewash.common.app.binaries;

import pl.mewash.common.app.settings.GeneralSettings;
import pl.mewash.common.app.settings.SettingsManager;
import pl.mewash.common.logging.api.FileLogger;
import pl.mewash.common.logging.api.LoggersProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BinariesManager {

    private final FileLogger fileLogger = LoggersProvider.getFileLogger();
    private SupportedPlatforms detectedPlatform;

    // --- check for binaries in system path and default directories  ---

    public Optional<BinariesInstallation> verifyBinariesDefaultInstallation() {
        GeneralSettings generalSettings = SettingsManager.loadSettings();

        BinariesInstallation savedInstallation = generalSettings.getBinariesInstallation();
        if (savedInstallation != null && savedInstallation.isConfirmed())
            return Optional.of(savedInstallation);

        Optional<BinariesInstallation> systemPathInstallation = verifySystemPath();
        if (systemPathInstallation.isPresent()) {
            persistInstallationWithMessage(systemPathInstallation.get());
            return systemPathInstallation;
        }

        Optional<BinariesInstallation> defaultDirInstallation = verifyDefaultDirs();
        if (defaultDirInstallation.isPresent()) {
            persistInstallationWithMessage(defaultDirInstallation.get());
            return defaultDirInstallation;
        }

        return Optional.empty();
    }

    // --- check for binaries in directory specified by user ---

    public Optional<BinariesInstallation> verifyBinariesAtUsersLocation(String usersLocation) {
        Optional<BinariesInstallation> installation = verifyInstallInDir(usersLocation);
        if (installation.isPresent()) {
            persistInstallationWithMessage(installation.get());
            return installation;
        } else {
            fileLogger.appendSingleLine("Tools verification failed on given location: " + usersLocation);
            return Optional.empty();
        }
    }

    // --- verify system path ---

    private Optional<BinariesInstallation> verifySystemPath() {
        fileLogger.appendSingleLine("Checking for binaries installation in SYSTEM PATH");

        Map<BinariesNames, Optional<String>> versionResponses = BinariesNames
            .getObligatorySet().stream()
            .map(binary -> Map.entry(binary, getSysPathVersionResponse(binary)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        boolean anyBinaryResponseFound = versionResponses.entrySet().stream()
            .anyMatch(entry -> entry.getValue().isPresent());

        if (!anyBinaryResponseFound) return Optional.empty();

        boolean sysPathInstallationResult = validateVersionsResultsWithResponse(versionResponses);

        return sysPathInstallationResult
            ? Optional.of(BinariesInstallation.confirmInSysPath(getPlatform()))
            : Optional.empty();
    }

    private Optional<String> getSysPathVersionResponse(BinariesNames binary) {
        String sysPathCommand = binary.getBinaryName(getPlatform());
        Optional<String> versionResponse = Optional.empty();

        try {
            versionResponse = runVersionCheckProcess(binary, sysPathCommand);

        } catch (IOException notFoundEx) {
            fileLogger.appendSingleLine(String
                .format("Binary: %s not found on system path", binary.getBinaryName(getPlatform())));

        } catch (Exception e) {
            fileLogger.logErrWithMessage("Unexpected error on binary check in system path: ", e, true);
            fileLogger.logErrStackTrace(e, true);
        }
        return versionResponse;
    }

    // --- verify directories ---

    private Optional<BinariesInstallation> verifyDefaultDirs() {
        return getPlatform().getDefaultToolPaths().stream()
            .map(SupportedPlatforms.DefaultToolPath::compilePath)
            .map(this::verifyInstallInDir)
            .flatMap(Optional::stream)
            .findFirst();
    }

    private Optional<BinariesInstallation> verifyInstallInDir(String location) {
        fileLogger.appendSingleLine("Verifying binaries at location: " + location);

        int obligatoryCount = BinariesNames.getObligatoryCount();
        Map<BinariesNames, Path> existingPaths = getExistingBinariesPaths(location);

        if (existingPaths.size() < obligatoryCount) {
            if (existingPaths.isEmpty()) fileLogger
                .appendSingleLine("No binaries found at above location");
            if (!existingPaths.isEmpty()) fileLogger
                .appendSingleLine("MISSING BINARIES! Found only " + existingPaths.size());
            return Optional.empty();
        }

        Map<BinariesNames, Optional<String>> dirCheckResponses = getDirPathVersionResponses(existingPaths);
        boolean versionCheckResult = validateVersionsResultsWithResponse(dirCheckResponses);

        return versionCheckResult
            ? Optional.of(BinariesInstallation.confirmInDirectory(getPlatform(), location))
            : Optional.empty();
    }

    private Map<BinariesNames, Path> getExistingBinariesPaths(String location) {
        return BinariesNames.getObligatorySet().stream()
            .map(requiredBinary -> Map
                .entry(requiredBinary, requiredBinary.resolveExecutablePath(location, getPlatform())))
            .filter(entry -> Files.exists(entry.getValue()))
            .collect(Collectors
                .toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<BinariesNames, Optional<String>> getDirPathVersionResponses(Map<BinariesNames, Path> existingPaths) {
        return existingPaths.entrySet().stream()
            .map(entry -> Map
                .entry(entry.getKey(), getDirPathVersionResponse(entry.getKey(), entry.getValue())))
            .collect(Collectors
                .toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<String> getDirPathVersionResponse(BinariesNames binary, Path binaryPath) {
        String dirPathCommand = binaryPath.toAbsolutePath().toString();
        Optional<String> versionResponse = Optional.empty();

        try {
            versionResponse = runVersionCheckProcess(binary, dirPathCommand);

        } catch (Exception e) {
            fileLogger.logErrWithMessage("Error of version check: " + dirPathCommand, e, true);
            fileLogger.logErrStackTrace(e, true);
        }
        return versionResponse;
    }

    // --- verify versions ---

    private boolean validateVersionsResultsWithResponse(Map<BinariesNames, Optional<String>> versionCheckResponses) {
        versionCheckResponses.entrySet().stream()
            .filter(entry -> entry.getValue().isPresent())
            .forEach(entry -> fileLogger
                .appendMultiLineStringList(List.of(
                    String.format("Binary: %s version check: ", entry.getKey().getBinaryName(getPlatform())),
                    entry.getValue().get())));

        Set<BinariesNames> notResponsiveBinaries = versionCheckResponses.entrySet().stream()
            .filter(entry -> entry.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        if (!notResponsiveBinaries.isEmpty()) {
            fileLogger.appendSingleLine("Some of found binaries did not return version info!");
            notResponsiveBinaries.forEach(binary -> fileLogger
                .appendSingleLine("Please verify version and installation of: " + binary.getBinaryName(getPlatform())));
            return false;
        }
        return true;
    }

    private Optional<String> runVersionCheckProcess(BinariesNames binary, String binCommand)
                                                                            throws IOException, InterruptedException {
        StringBuilder responseBuilder = new StringBuilder();

        ProcessBuilder builder = new ProcessBuilder(binCommand, binary.getVersionCommand());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append("\n").append(line);
        }
        process.waitFor();

        return responseBuilder.isEmpty()
            ? Optional.empty()
            : Optional.of(responseBuilder.toString());
    }

    // --- common methods ---

    private SupportedPlatforms getPlatform() {
        if (detectedPlatform != null) return detectedPlatform;

        detectedPlatform = SupportedPlatforms.getCurrentPlatform();
        return detectedPlatform;
    }

    private void persistInstallationWithMessage(BinariesInstallation installation) {
        fileLogger.appendSingleLine(installation.getInstallationMessage());
        try {
            SettingsManager.saveBinariesInstallation(installation);
        } catch (Exception e) {
            String persistFailed = """
                --- Failed to save confirmed binaries installation configuration! ---
                --- Verify if application has write privileges! ---
                Error message and stack trace:""";
            fileLogger.logErrWithMessage(persistFailed, e, true);
            fileLogger.logErrStackTrace(e, true);
        }
    }
}
