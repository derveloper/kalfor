package cc.vileda.kalfor;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;


public class Kalfor {
    private final Endpoint endpoint;

    public Kalfor(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void listen(final int port) {
        final Vertx vertx = Vertx.vertx();
        final DeploymentOptions deploymentOptions = new DeploymentOptions()
          .setConfig(new JsonObject()
            .put("ssl", endpoint.isSSL())
            .put("host", endpoint.host())
            .put("port", endpoint.port())
            .put("listenPort", port));

        vertx.deployVerticle(
          KalforVerticle.class.getName(),
          deploymentOptions
        );
    }
}
