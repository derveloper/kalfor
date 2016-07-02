package cc.vileda.kalfor;

import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import java.net.MalformedURLException;


public class Kalfor {
    private final Endpoint endpoint;

    public Kalfor(final String url) throws MalformedURLException {
        this.endpoint = new Endpoint(url);
    }

    public void listen(final int port) {
        final Vertx vertx = Vertx.vertx();

        final KalforOptions kalforOptions = new KalforOptions(endpoint.isSSL(), endpoint.host(), endpoint.port(), port);
        final Observable<String> verticle = RxHelper.deployVerticle(vertx, new KalforVerticle(kalforOptions));
        verticle.subscribe(System.out::println);
    }
}
