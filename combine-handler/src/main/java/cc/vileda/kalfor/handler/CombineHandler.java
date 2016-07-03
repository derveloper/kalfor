package cc.vileda.kalfor.handler;

import cc.vileda.kalfor.core.Endpoint;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.*;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class CombineHandler implements Handler<RoutingContext>
{
	private final static Logger LOGGER = LoggerFactory.getLogger(CombineHandler.class);
	private final Vertx vertx;

	public CombineHandler(final Vertx vertx)
	{
		this.vertx = vertx;
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

	private Func1<KalforRequest, Observable<Context>> makeRequest(HttpServerRequest request)
	{
		return pair -> {
			try {
				final Endpoint endpoint = new Endpoint(pair.proxyBaseUrl);
				request.headers().remove("Origin");
				request.headers().remove("Host");
				request.headers().remove("Close");
				request.headers().remove("Content-Length");

				final HttpClient httpClient = getHttpClient(endpoint);

				return Observable.from(pair.proxyRequests)
						.flatMap(kalforProxyRequest -> {
							final ObservableFuture<Context> observableFuture = new ObservableFuture<>();

							final HttpClientRequest httpClientRequest = httpClient.get(
									endpoint.port(),
									endpoint.host(),
									kalforProxyRequest.path,
									handleClientResponse(observableFuture, kalforProxyRequest.key)
							);

							httpClientRequest.exceptionHandler(Throwable::printStackTrace);

							httpClientRequest
									.putHeader("Host", endpoint.host())
									.putHeader("Connection", "close");
							httpClientRequest.headers().addAll(request.headers());
							httpClientRequest.end();

							return observableFuture;
						})
						.doOnUnsubscribe(httpClient::close);
			}
			catch (MalformedURLException e) {
				return Observable.error(e);
			}
		};
	}

	private Action1<JsonObject> sendResponse(final HttpServerRequest request, final HttpServerResponse response)
	{
		return entries -> response
				.putHeader("content-type", request.getHeader("content-type"))
				.end(entries.encodePrettily());
	}

	private HttpClient getHttpClient(final Endpoint endpoint)
	{
		final HttpClientOptions httpClientOptions = new HttpClientOptions()
				.setDefaultHost(endpoint.host())
				.setSsl(endpoint.isSSL())
				.setTrustAll(true)
				.setVerifyHost(false)
				.setDefaultPort(endpoint.port());
		return vertx.createHttpClient(httpClientOptions);
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

	private Observable<KalforRequest> transformRequest(final Buffer buffer)
	{
		LOGGER.info("request body: {}", buffer.toString());
		return Observable.from(buffer.toJsonArray()
				.stream()
				.map(object -> Json.decodeValue(((JsonObject) object).encode(), KalforRequest.class))
				.collect(Collectors.toList()));
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
