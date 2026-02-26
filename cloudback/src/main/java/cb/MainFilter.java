package cb;

import io.quarkus.vertx.web.RouteFilter;
import jakarta.enterprise.context.control.ActivateRequestContext;
import io.vertx.ext.web.RoutingContext;

public class MainFilter {
    
    @RouteFilter
    @ActivateRequestContext
    void filter(RoutingContext rc) {
        rc.response()
            .setStatusCode(200)
            .send("OK\n");
        return;
        //rc.next();
    }
}