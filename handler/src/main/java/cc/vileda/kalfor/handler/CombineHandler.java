package cc.vileda.kalfor.handler;

import cc.vileda.kalfor.core.Endpoint;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rxjava.core.Future;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.*;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;


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

		Observable.just(event.getBodyAsJsonArray())
				.flatMap(this::transformRequest)
				.flatMap(proxyRequest(request))
				.timeout(5, TimeUnit.SECONDS)
				.reduce(new JsonObject(), this::aggregateResponse)
				.doOnError(Throwable::printStackTrace)
				.onErrorReturn(throwable -> new JsonObject())
				.subscribe(sendResponse(request, response));
	}

	private Func1<KalforRequest, Observable<Context>> proxyRequest(final HttpServerRequest request)
	{
		return pair -> {
			try {
				final Endpoint endpoint = new Endpoint(pair.proxyBaseUrl);
				final HttpClient httpClient = getHttpClient(endpoint);

				removeRequestHeaders(request);

				return Observable.from(pair.proxyRequests)
						.flatMap(getProxyPath(pair.headers, request, endpoint, httpClient))
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

	private void removeRequestHeaders(final HttpServerRequest request)
	{
		request.headers().remove("Origin");
		request.headers().remove("Host");
		request.headers().remove("Close");
		request.headers().remove("Content-Length");
	}

	private Func1<KalforProxyRequest, Observable<Context>> getProxyPath(
			final List<KalforProxyHeader> headers, final HttpServerRequest request,
			final Endpoint endpoint,
			final HttpClient httpClient)
	{
		return kalforProxyRequest -> {
			final ObservableFuture<Context> observableFuture = new ObservableFuture<>();

			final HttpClientRequest httpClientRequest = httpClient.get(
					endpoint.port(),
					endpoint.host(),
					kalforProxyRequest.path,
					handleClientResponse(observableFuture, kalforProxyRequest.key)
			);

			Optional.ofNullable(headers).orElse(emptyList()).forEach(header -> {
				httpClientRequest.putHeader(header.name, header.value);
				request.headers().remove(header.name);
			});

			httpClientRequest.exceptionHandler(Throwable::printStackTrace);

			httpClientRequest
					.putHeader("Host", endpoint.host())
					.putHeader("Connection", "close");
			httpClientRequest.headers().addAll(request.headers());
			httpClientRequest.end();

			return observableFuture;
		};
	}

	private Handler<HttpClientResponse> handleClientResponse(final ObservableFuture<Context> observableFuture, final String name)
	{
		return httpClientResponse -> {
			LOGGER.debug("status code: {}", HttpResponseStatus.valueOf(httpClientResponse.statusCode()));
			httpClientResponse.exceptionHandler(Throwable::printStackTrace);
			final MultiMap headers = httpClientResponse.headers();
			LOGGER.debug("headers: {}", Json.encodePrettily(headers.names().stream().parallel().map(headers::getAll).collect(Collectors.toList())));
			httpClientResponse.bodyHandler(buffer -> observableFuture.toHandler()
					.handle(io.vertx.core.Future.succeededFuture(new Context(name, buffer))));
		};
	}

	private Observable<KalforRequest> transformRequest(final JsonArray requestArray)
	{
		LOGGER.debug("request body: {}", requestArray.encode());
		return Observable.from(requestArray
				.stream().parallel()
				.map(object -> Json.decodeValue(((JsonObject) object).encode(), KalforRequest.class))
				.collect(Collectors.toList()));
	}

	private JsonObject aggregateResponse(final JsonObject entries, final Context context)
	{
		final String path = context.name;
		final String body = context.buffer.toString();

		LOGGER.debug("response body: {}", body);

		return body.trim().startsWith("{")
				? entries.put(path, new JsonObject(body))
				: entries;
	}
}
