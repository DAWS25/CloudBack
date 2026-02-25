package cloudback;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static cloudback.Logs.*;

public class Init {
    @Inject CBConfig config;

    // matches: "Type: AWS::Something" (allow whitespace)
    private static final Pattern CF_TYPE_PATTERN = Pattern.compile("(?m)^\\s*Type\\s*:\\s*AWS::");

    // avoid reading gigantic files (tweak as you like)
    private static final long MAX_BYTES_TO_SCAN = 2 * 1024 * 1024; // 2 MB
    private static final int MAX_LINES_TO_SCAN = 50_000;

    public void init(@Observes StartupEvent ev) {
        log().trace("CloudBack initialization started");
        var files = lookupSauceFiles();
        log().debugf("Sauce files found [%s]:", files.size());
        files.forEach(f -> log().debugf("  %s", f.toAbsolutePath()));
        log().debug("CloudBack initialization completed");
    }

    private List<Path> lookupSauceFiles() {
        Path basePath = config.basePath();
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
                            log().debugf("Found CloudFormation template: %s", p.toAbsolutePath());
                        }
                    } catch (Exception e) {
                        // continue scanning other files even if one fails
                        log().warnf("Failed to inspect %s: %s", p.toAbsolutePath(), e.toString());
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
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private boolean looksLikeCloudFormationTemplate(Path p) throws IOException {
        // quick size gate
        long size = Files.size(p);
        if (size <= 0) return false;
        if (size > MAX_BYTES_TO_SCAN) {
            log().tracef("Skipping large file (%d bytes): %s", size, p.toAbsolutePath());
            return false;
        }

        // scan line-by-line to avoid loading whole file
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String line;
            int lines = 0;

            while ((line = br.readLine()) != null) {
                lines++;
                // fast pre-check avoids regex cost most of the time
                if (line.contains("Type") && line.contains("AWS::")) {
                    if (CF_TYPE_PATTERN.matcher(line).find()) return true;
                    // if "Type:" is indented or split oddly, regex on line still catches common cases
                }

                if (lines >= MAX_LINES_TO_SCAN) {
                    log().tracef("Stopping scan after %d lines: %s", MAX_LINES_TO_SCAN, p.toAbsolutePath());
                    break;
                }
            }
        }

        return false;
    }
}