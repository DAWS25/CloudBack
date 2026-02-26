package cb.config;

import java.nio.file.Path;
import java.util.UUID;

import com.github.f4b6a3.uuid.UuidCreator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CloudBackConfig {
    String executionId;

    @Inject
    ICloudBackConfig config;

    public String getExecutionId() {
        if (executionId == null) synchronized (this) {
            if (config.executionId().isPresent()) {
                executionId = config.executionId().get();
            } else {  
                executionId = generateTimeSortedId();
            }
        }
        return executionId;
    }

    private String generateTimeSortedId() {
        var uuid = UuidCreator.getTimeOrderedEpoch();
        return uuid.toString();
    }

    public Path getBasePath() {
        return config.basePath().orElse(Path.of("."));
    }
}
