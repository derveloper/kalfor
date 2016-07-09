package cc.vileda.kalfor.handler

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.rx.java.ObservableFuture
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.http.HttpClient
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.rxjava.core.http.HttpServerRequest
import io.vertx.rxjava.core.http.HttpServerResponse
import io.vertx.rxjava.ext.web.RoutingContext
import rx.Observable
import java.net.MalformedURLException
import java.util.concurrent.TimeUnit


class CombineHandler(private val vertx: Vertx) : Handler<RoutingContext> {

    override fun handle(event: RoutingContext) {
        val request = event.request()
        val response = event.response()

        Observable.from(event.bodyAsJsonArray)
                .filter { it is JsonObject }
                .map { it as JsonObject }
                .map { Json.decodeValue(it.encode(), KalforRequest::class.java) }
                .flatMap({ makeRequests(it, request) })
                .timeout(5, TimeUnit.SECONDS)
                .reduce(JsonObject(), aggregateResponse())
                .doOnError { it.printStackTrace() }
                .onErrorReturn { throwable -> JsonObject() }
                .subscribe(respondToClient(request, response))
    }

    private fun aggregateResponse(): (JsonObject, Context) -> JsonObject = { jsonResponse, context ->
        val path = context.name
        val body = context.buffer.toString()

        LOGGER.debug("serverResponse body: {}", body)

        if (body.trim { it <= ' ' }.startsWith("{"))
            jsonResponse.put(path, JsonObject(body))
        else
            jsonResponse
    }

    private fun respondToClient(serverRequest: HttpServerRequest, serverResponse: HttpServerResponse): (JsonObject) -> Unit = {
        serverResponse.putHeader("content-type", serverRequest.getHeader("content-type")).end(it.encodePrettily())
    }

    private fun makeRequests(kalforRequest: KalforRequest, request: HttpServerRequest): Observable<Context>? {
        return try {
            proxyRequest(kalforRequest, request)
        } catch (e: MalformedURLException) {
            Observable.error<Context>(e)
        }
    }

    private fun proxyRequest(kalforRequest: KalforRequest, request: HttpServerRequest): Observable<Context>? {
        val endpoint = Endpoint(kalforRequest.proxyBaseUrl)
        val httpClient = getHttpClient(endpoint)

        removeRequestHeaders(request)

        return Observable.from<KalforProxyRequest>(kalforRequest.proxyRequests)
                .flatMap(makeSingleRequest(endpoint, kalforRequest.headers, httpClient, request))
                .doOnUnsubscribe({ httpClient.close() })
    }

    private fun makeSingleRequest(
            endpoint: Endpoint,
            headers: List<KalforProxyHeader>?,
            httpClient: HttpClient,
            request: HttpServerRequest
    ): (KalforProxyRequest) -> ObservableFuture<Context> = {
        val observableFuture = ObservableFuture<Context>()

        @Suppress("ReplaceGetOrSet")
        val httpClientRequest = httpClient.get(
                endpoint.port(),
                endpoint.host(),
                it.path,
                handleResponse(it.key, observableFuture))

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

    private fun handleResponse(key: String, contextFuture: ObservableFuture<Context>): (HttpClientResponse) -> Unit = {
        LOGGER.debug("status code: {}", HttpResponseStatus.valueOf(it.statusCode()))
        it.exceptionHandler({ it.printStackTrace() })
        val responseHeaders = it.headers()
        LOGGER.debug("headers: {}", Json.encodePrettily(responseHeaders.names().map({ responseHeaders.getAll(it) })))
        it.bodyHandler({ buffer -> contextFuture.toHandler().handle(Future.succeededFuture(Context(key, buffer))) })
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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CombineHandler::class.java)
    }
}
