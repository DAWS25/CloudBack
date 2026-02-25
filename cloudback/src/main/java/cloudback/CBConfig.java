package cloudback;

import java.nio.file.Path;
import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "cb")
public interface CBConfig {
    @WithDefault(".")
    Path basePath();
}