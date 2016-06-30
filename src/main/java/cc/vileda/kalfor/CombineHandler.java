package cc.vileda.kalfor;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.http.*;
import io.vertx.rxjava.ext.web.RoutingContext;
import javafx.util.Pair;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

class CombineHandler implements Handler<RoutingContext> {
    private final HttpClient httpClient;

    CombineHandler(final HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void handle(RoutingContext event) {
        final HttpServerRequest request = event.request();
        final HttpServerResponse response = event.response();

        final List<Pair<String, String>>
          body =
          event.getBodyAsJsonArray()
            .stream()
            .map(object -> (JsonObject) object)
            .flatMap(entries -> entries.getMap().entrySet().stream())
            .filter(r -> r.getValue() instanceof String)
            .map(stringObjectEntry -> new Pair<>(stringObjectEntry.getKey(), (String) stringObjectEntry.getValue()))
            .collect(Collectors.toList());

        System.out.println(body);

        ObservableFuture.from(body)
          .flatMap(makeRequest(request))
          .timeout(5, TimeUnit.SECONDS)
          .reduce(new JsonObject(), aggregateResponse())
          .doOnError(Throwable::printStackTrace)
          .onErrorReturn(throwable -> new JsonObject())
          .subscribe(entries -> response.end(entries.encodePrettily()));
    }

    private Func2<JsonObject, Context, JsonObject> aggregateResponse() {
        return (entries, context) -> {
            final String path = context.path;
            final String body = context.buffer.toString();
            System.out.println(body);

            if (body.trim().startsWith("{")) {
                return entries.put(path, new JsonObject(body));
            } else {
                return entries;
            }
        };
    }

    private Func1<Pair<String, String>, Observable<? extends Context>> makeRequest(HttpServerRequest request) {
        return pair -> {
            final ObservableFuture<Context> observableFuture = new ObservableFuture<>();
            final String name = pair.getKey();
            final String path = pair.getValue();
            final HttpClientRequest httpClientRequest = httpClient.get(path, handleClientResponse(observableFuture, name, path));

            httpClientRequest.exceptionHandler(Throwable::printStackTrace);
            request.headers().remove("Origin");
            httpClientRequest.headers().addAll(request.headers());
            httpClientRequest.putHeader("Connection", "close").end();
            return observableFuture;
        };
    }

    private Handler<HttpClientResponse> handleClientResponse(ObservableFuture<Context> observableFuture, String name, String path) {
        return httpClientResponse -> {
            {
                System.out.println(httpClientResponse.statusCode());
                httpClientResponse.exceptionHandler(Throwable::printStackTrace);
                httpClientResponse.bodyHandler(buffer -> observableFuture.toHandler()
                  .handle(io.vertx.core.Future.succeededFuture(new Context(name, path, buffer))));
            }
        };
    }
}
