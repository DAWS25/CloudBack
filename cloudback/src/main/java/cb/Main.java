package cb;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;
import static cb.Logs.*;

@QuarkusMain
public class Main {

    public static void main(String[] args) {
        log().debug("CloudBack started");
        Quarkus.run(args);
        log().debug("CloudBack completed");
    }
    
}
