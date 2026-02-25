package cloudback;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class Main {

    public static void main(String[] args) {
        Log.info("Starting CloudBack...");
        var pwd = System.getProperty("user.dir");
        Log.info("Current working directory: " + pwd);
        Quarkus.run(args);
    }
    
}
