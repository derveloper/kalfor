package cc.vileda.kalfor;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;


public class KalforVerticle extends AbstractVerticle {
    private final KalforOptions kalforOptions;

    public KalforVerticle(final KalforOptions kalforOptions) {
        this.kalforOptions = kalforOptions;
    }

    @Override
    public void start() throws Exception {
        final HttpServer httpServer = vertx.createHttpServer();
        final HttpClient httpClient = createHttpClient();

        final Router router = Router.router(vertx);
        router.route().handler(
          CorsHandler.create("*").allowedHeader("authorization")
        );

        router.post("/combine").handler(new CombineHandler(httpClient, kalforOptions.proxyHost));
        router.get("/combine").handler(new CombineHandler(httpClient, kalforOptions.proxyHost));

        httpServer
          .requestHandler(router::accept)
          .listen(kalforOptions.listenPort);
    }

    private HttpClient createHttpClient() {
        final HttpClientOptions httpClientOptions = new HttpClientOptions()
          .setDefaultHost(kalforOptions.proxyHost)
          .setSsl(kalforOptions.ssl)
          .setDefaultPort(kalforOptions.proxyPort);

        return vertx.createHttpClient(httpClientOptions);
    }
}
