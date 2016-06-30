package cc.vileda.kalfor;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;


public class KalforVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        final HttpServer httpServer = vertx.createHttpServer();
        final HttpClient httpClient = createHttpClient();

        final Router router = Router.router(vertx);
        router.route().handler(
          CorsHandler.create("*").allowedHeader("authorization")
        );

        router.route().handler(BodyHandler.create());

        router.post("/combine").handler(new CombineHandler(httpClient));
        router.get("/combine").handler(new CombineHandler(httpClient));

        httpServer
          .requestHandler(router::accept)
          .listen(config().getInteger("listenPort", 8080));
    }

    private HttpClient createHttpClient() {
        final HttpClientOptions httpClientOptions = new HttpClientOptions()
          .setDefaultHost(config().getString("host"))
          .setSsl(config().getBoolean("ssl", false))
          .setDefaultPort(config().getInteger("port"));

        return vertx.createHttpClient(httpClientOptions);
    }
}
