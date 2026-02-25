package cloudback;

import org.jboss.logging.Logger;

public class Logs {
    static final Logger rootLogger = Logger.getLogger("cb");
    static Logger log() {
        return rootLogger;
    }

}
