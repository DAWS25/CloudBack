package cloudback;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import static cloudback.Logs.*;

@QuarkusMain
public class Main {

    public static void main(String[] args) {
        log().info("Starting CloudBack...");
        log().info("Current working directory: [REDACTED]");
        Quarkus.run(args);
    }
    
}
