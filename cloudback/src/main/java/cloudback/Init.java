package cloudback;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

public class Init {
    @Inject CBConfig config;
 
    public void init(@Observes StartupEvent ev) {
        System.out.println("Init called");
        var basePath = config.basePath();
        Log.info("Base path: " + basePath.toAbsolutePath());
    }  
}
