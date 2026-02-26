package cb;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cb.config.CloudBackConfig;

import static cb.Logs.*;

@ApplicationScoped
public class Init {
    @Inject
    CloudBackConfig config;

    // matches: "Type: AWS::Something" (allow whitespace)
    private static final Pattern CF_TYPE_PATTERN = Pattern.compile("(?m)^\\s*Type\\s*:\\s*AWS::");

    // avoid reading gigantic files (tweak as you like)
    private static final long MAX_BYTES_TO_SCAN = 2 * 1024 * 1024; // 2 MB
    private static final int MAX_LINES_TO_SCAN = 50_000;

    public void init(@Observes StartupEvent ev) {
        log().trace("CloudBack initialization started");
        var executionId = config.getExecutionId();
        log().debugf("Execution Id: %s", executionId);
        log().debugf("Base Path: %s", config.getBasePath().toAbsolutePath());
        var files = lookupSauceFiles();
        log().debugf("Sauce files found [%s]:", files.size());
        files.forEach(f -> log().debugf("  %s", f.toAbsolutePath()));
        log().debug("CloudBack initialization completed");
    }

    private List<Path> lookupSauceFiles() {
        Path basePath = config.getBasePath();
        Path abs = basePath.toAbsolutePath();

        log().infof("Looking up CloudFormation templates in %s", abs);

        if (!Files.exists(basePath)) {
            log().warnf("Base path does not exist: %s", abs);
            return List.of();
        }
        if (!Files.isDirectory(basePath)) {
            log().warnf("Base path is not a directory: %s", abs);
            return List.of();
        }

        List<Path> found = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(basePath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(this::isYamlFile)
                .forEach(p -> {
                    try {
                        if (looksLikeCloudFormationTemplate(p)) {
                            found.add(p);
                            log().debugf("Found CloudFormation template: %s", sanitizeForLog(p.toAbsolutePath().toString()));
                        }
                    } catch (IOException e) {
                        log().warnf("Failed to inspect %s: %s", sanitizeForLog(p.toAbsolutePath().toString()), e.getMessage());
                    }
                });
        } catch (IOException e) {
            log().errorf("Failed walking base path %s: %s", abs, e.toString());
            return List.of();
        }

        if (found.isEmpty()) {
            log().info("No CloudFormation templates found.");
        } else {
            log().infof("CloudFormation templates found: %d", found.size());
        }
        return found;
    }

    private boolean isYamlFile(Path p) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private boolean looksLikeCloudFormationTemplate(Path p) throws IOException {
        // Validate path to prevent traversal
        Path normalized = p.toAbsolutePath().normalize();
        Path baseNormalized = config.getBasePath().toAbsolutePath().normalize();
        if (!normalized.startsWith(baseNormalized)) {
            log().warnf("Path traversal attempt detected: %s", sanitizeForLog(p.toString()));
            return false;
        }

        // quick size gate
        long size = Files.size(p);
        if (size <= 0) return false;
        if (size > MAX_BYTES_TO_SCAN) {
            log().tracef("Skipping large file (%d bytes): %s", size, sanitizeForLog(p.toAbsolutePath().toString()));
            return false;
        }

        // scan line-by-line to avoid loading whole file
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            int lines = 0;

            while ((line = br.readLine()) != null && lines < MAX_LINES_TO_SCAN) {
                lines++;
                // fast pre-check avoids regex cost most of the time
                if (line.contains("Type") && line.contains("AWS::")) {
                    if (CF_TYPE_PATTERN.matcher(line).find()) return true;
                    // if "Type:" is indented or split oddly, regex on line still catches common cases
                }
            }

            if (lines >= MAX_LINES_TO_SCAN) {
                log().tracef("Stopped scan after %d lines: %s", MAX_LINES_TO_SCAN, sanitizeForLog(p.toAbsolutePath().toString()));
            }
        }

        return false;
    }

    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replace("\n", "").replace("\r", "");
    }
}