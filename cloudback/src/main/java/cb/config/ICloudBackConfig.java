package cb.config;

import java.nio.file.Path;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "cb")
public interface ICloudBackConfig {
    Optional<String> executionId();
    Optional<Path> basePath();
}
