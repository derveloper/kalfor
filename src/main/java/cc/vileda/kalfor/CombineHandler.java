package cc.vileda.kalfor;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.*;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Observable;
import rx.functions.Func1;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


class CombineHandler implements Handler<RoutingContext>
{
		private final HttpClient httpClient;
		private final String proxyHost;

		CombineHandler(final HttpClient httpClient, final String proxyHost)
		{
				this.httpClient = httpClient;
				this.proxyHost = proxyHost;
		}

		@Override
		public void handle(RoutingContext event)
		{
				final HttpServerRequest request = event.request();
				final HttpServerResponse response = event.response();

				event.request()
						.toObservable()
						.flatMap(this::transformRequest)
						.flatMap(makeRequest(request))
						.timeout(5, TimeUnit.SECONDS)
						.reduce(new JsonObject(), this::aggregateResponse)
						.doOnError(Throwable::printStackTrace)
						.onErrorReturn(throwable -> new JsonObject())
						.subscribe(entries -> response.end(entries.encodePrettily()));
		}

		private Func1<? super Map.Entry<String, String>, ? extends Observable<? extends Context>> makeRequest(HttpServerRequest request)
		{
				return pair -> {
						final ObservableFuture<Context> observableFuture = new ObservableFuture<>();
						final String name = pair.getKey();
						final String path = pair.getValue();
						final HttpClientRequest httpClientRequest = httpClient.get(path, handleClientResponse(observableFuture, name, path));

						httpClientRequest.exceptionHandler(Throwable::printStackTrace);
						request.headers().remove("Origin");
						httpClientRequest.headers().addAll(request.headers());
						httpClientRequest.putHeader("Connection", "close").putHeader("Host", proxyHost).end();
						return observableFuture;
				};
		}

		private Handler<HttpClientResponse> handleClientResponse(ObservableFuture<Context> observableFuture, String name, String path)
		{
				return httpClientResponse -> {
						{
								System.out.println(httpClientResponse.statusCode());
								httpClientResponse.exceptionHandler(Throwable::printStackTrace);
								httpClientResponse.bodyHandler(buffer -> observableFuture.toHandler()
										.handle(io.vertx.core.Future.succeededFuture(new Context(name, path, buffer))));
						}
				};
		}

		private Observable<? extends Map.Entry<String, String>> transformRequest(Buffer buffer)
		{
				return Observable.from(buffer.toJsonArray()
						.stream()
						.map(object -> (JsonObject) object)
						.flatMap(entries1 -> entries1.getMap().entrySet().stream())
						.filter(r -> r.getValue() instanceof String)
						.map(this::castValueToString)
						.collect(Collectors.toList()));
		}

		private Map.Entry<String, String> castValueToString(final Map.Entry<String, Object> pair)
		{
				return new AbstractMap.SimpleEntry<>(pair.getKey(), (String) pair.getValue());
		}

		private JsonObject aggregateResponse(final JsonObject entries, final Context context)
		{
				final String path = context.path;
				final String body = context.buffer.toString();
				System.out.println(body);

				return body.trim().startsWith("{")
						? entries.put(path, new JsonObject(body))
						: entries;
		}
}
