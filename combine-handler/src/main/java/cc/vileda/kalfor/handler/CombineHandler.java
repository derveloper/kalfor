package cc.vileda.kalfor.handler;

import cc.vileda.kalfor.core.KalforOptions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.*;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class CombineHandler implements Handler<RoutingContext>
{
	private final static Logger LOGGER = LoggerFactory.getLogger(CombineHandler.class);
	private final HttpClient httpClient;
	private final KalforOptions kalforOptions;

	public CombineHandler(final HttpClient httpClient, final KalforOptions kalforOptions)
	{
		this.httpClient = httpClient;
		this.kalforOptions = kalforOptions;
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
				.subscribe(sendResponse(request, response));
	}

	private Func1<Map.Entry<String, String>, Observable<Context>> makeRequest(HttpServerRequest request)
	{
		return pair -> {
			final ObservableFuture<Context> observableFuture = new ObservableFuture<>();
			final String name = pair.getKey();
			final String path = pair.getValue();
			final HttpClientRequest httpClientRequest = httpClient
					.get(kalforOptions.proxyPort, kalforOptions.proxyHost, path, handleClientResponse(observableFuture, name));

			httpClientRequest.exceptionHandler(Throwable::printStackTrace);

			request.headers().remove("Origin");
			request.headers().remove("Host");
			request.headers().remove("Close");
			request.headers().remove("Content-Length");
			httpClientRequest
					.putHeader("Host", kalforOptions.proxyHost)
					.putHeader("Connection", "close");
			httpClientRequest.headers().addAll(request.headers());
			httpClientRequest.end();
			return observableFuture;
		};
	}

	private Action1<JsonObject> sendResponse(final HttpServerRequest request, final HttpServerResponse response)
	{
		return entries -> response
				.putHeader("content-type", request.getHeader("content-type"))
				.end(entries.encodePrettily());
	}

	private Handler<HttpClientResponse> handleClientResponse(final ObservableFuture<Context> observableFuture, final String name)
	{
		return httpClientResponse -> {
			LOGGER.info("status code: {}", HttpResponseStatus.valueOf(httpClientResponse.statusCode()));
			httpClientResponse.exceptionHandler(Throwable::printStackTrace);
			final MultiMap headers = httpClientResponse.headers();
			LOGGER.info("headers: {}", Json.encodePrettily(headers.names().stream().map(headers::getAll).collect(Collectors.toList())));
			httpClientResponse.bodyHandler(buffer -> observableFuture.toHandler()
					.handle(io.vertx.core.Future.succeededFuture(new Context(name, buffer))));
		};
	}

	private Observable<Map.Entry<String, String>> transformRequest(final Buffer buffer)
	{
		LOGGER.info("request body: {}", buffer.toString());
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
		final String path = context.name;
		final String body = context.buffer.toString();

		LOGGER.info("response body: {}", body);

		return body.trim().startsWith("{")
				? entries.put(path, new JsonObject(body))
				: entries;
	}
}
