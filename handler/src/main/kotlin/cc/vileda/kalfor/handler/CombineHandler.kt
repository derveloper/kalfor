package cc.vileda.kalfor.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.rx.java.ObservableFuture
import io.vertx.rxjava.core.MultiMap
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.http.*
import io.vertx.rxjava.ext.web.RoutingContext
import rx.Observable
import rx.functions.Action1
import rx.functions.Func1

import java.net.MalformedURLException
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import java.util.Collections.emptyList


class CombineHandler(private val vertx: Vertx) : Handler<RoutingContext> {

    override fun handle(event: RoutingContext) {
        val request = event.request()
        val response = event.response()

        Observable.just(event.bodyAsJsonArray)
                .flatMap { this.transformRequest(it) }
                .flatMap({ makeRequests(it, request) })
                .timeout(5, TimeUnit.SECONDS)
                .reduce(JsonObject(), { entries, context -> this.aggregateResponse(entries, context) })
                .doOnError { it.printStackTrace() }
                .onErrorReturn { throwable -> JsonObject() }
                .subscribe({ response.putHeader("content-type", request.getHeader("content-type")).end(it.encodePrettily()) })
    }

    private fun makeRequests(it: KalforRequest, request: HttpServerRequest): Observable<Context>? {
        return try {
            val endpoint = Endpoint(it.proxyBaseUrl)
            val httpClient = getHttpClient(endpoint)

            removeRequestHeaders(request)

            Observable.from<KalforProxyRequest>(it.proxyRequests)
                    .flatMap(makeSingleRequest(endpoint, it.headers, httpClient, request))
                    .doOnUnsubscribe({ httpClient.close() })
        } catch (e: MalformedURLException) {
            Observable.error<Context>(e)
        }
    }

    private fun makeSingleRequest(endpoint: Endpoint,
                                  headers: List<KalforProxyHeader>?,
                                  httpClient: HttpClient,
                                  request: HttpServerRequest)
            : (KalforProxyRequest) -> ObservableFuture<Context>
    {
        return {
            val observableFuture = ObservableFuture<Context>()

            val key = it.key
            val httpClientRequest = httpClient.get(
                    endpoint.port(),
                    endpoint.host(),
                    it.path,
                    handleResponse(key, observableFuture))

            headers?.forEach { header ->
                httpClientRequest.putHeader(header.name, header.value)
                request.headers().remove(header.name)
            }

            httpClientRequest.exceptionHandler { it.printStackTrace() }

            httpClientRequest.putHeader("Host", endpoint.host()).putHeader("Connection", "close")
            httpClientRequest.headers().addAll(request.headers())
            httpClientRequest.end()

            observableFuture
        }
    }

    private fun handleResponse(key: String, observableFuture: ObservableFuture<Context>): (HttpClientResponse) -> Unit {
        return {
            LOGGER.debug("status code: {}", HttpResponseStatus.valueOf(it.statusCode()))
            it.exceptionHandler({ it.printStackTrace() })
            val responseHeaders = it.headers()
            LOGGER.debug("headers: {}", Json.encodePrettily(responseHeaders.names().map({ responseHeaders.getAll(it) })))
            it.bodyHandler({ buffer -> observableFuture.toHandler().handle(Future.succeededFuture(Context(key, buffer))) })
        }
    }

    private fun getHttpClient(endpoint: Endpoint): HttpClient {
        val httpClientOptions = HttpClientOptions()
                .setDefaultHost(endpoint.host())
                .setSsl(endpoint.isSSL!!)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setDefaultPort(endpoint.port())
        return vertx.createHttpClient(httpClientOptions)
    }

    private fun removeRequestHeaders(request: HttpServerRequest) {
        request.headers().remove("Origin")
        request.headers().remove("Host")
        request.headers().remove("Close")
        request.headers().remove("Content-Length")
    }

    private fun transformRequest(requestArray: JsonArray): Observable<KalforRequest> {
        LOGGER.debug("request body: {}", requestArray.encode())
        return Observable.from(requestArray.map({ `object` ->
            Json.decodeValue((`object` as JsonObject).encode(), KalforRequest::class.java) }))
    }

    private fun aggregateResponse(entries: JsonObject, context: Context): JsonObject {
        val path = context.name
        val body = context.buffer.toString()

        LOGGER.debug("response body: {}", body)

        return if (body.trim { it <= ' ' }.startsWith("{"))
            entries.put(path, JsonObject(body))
        else
            entries
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CombineHandler::class.java)
    }
}
