package cc.vileda.kalfor.handler

import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.MultiMap
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.http.HttpClient
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.rxjava.core.http.HttpServerRequest
import io.vertx.rxjava.core.http.HttpServerResponse
import io.vertx.rxjava.ext.web.RoutingContext
import org.funktionale.memoization.memoize
import rx.Observable
import java.net.URI
import java.net.URLDecoder
import java.util.*

val LOGGER: Logger = LoggerFactory.getLogger(CombineHandler::class.java)


fun parseRequest(routingContext: RoutingContext, vertx: Vertx): Observable<ResponseContext> {
    return Observable.from(parseRequestStrategy(routingContext)
            .flatMap(executeRequest(vertx)))
            .doOnEach(LOGGER::debug)
            .doOnError { LOGGER.error(it.message, it) }
}

fun aggregateResponse() = { last: String, ctx: ResponseContext ->
    when (ctx.method) {
        HttpMethod.POST -> aggregateJsonResponse(last, ctx)
        else -> aggregatePlainTextResponse(last, ctx)
    }
}

private fun aggregatePlainTextResponse(last: String, ctx: ResponseContext) = last.plus("\n${ctx.body}")

private fun aggregateJsonResponse(last: String, ctx: ResponseContext): String {
    val jsonObject = if (last == "") JsonObject() else JsonObject(last)

    return try {
        val body = JsonObject(ctx.body)
        val key = body.fieldNames().first()
        JsonObject(TreeMap(jsonObject.put(key, body.getJsonObject(key)).map)).encodePrettily()
    } catch (e: Exception) {
        JsonObject(TreeMap(jsonObject.put(ctx.key, ctx.body).map)).encodePrettily()
    }
}

fun convertResponse(resp: ResponseContext): Observable<ResponseContext>? {
    return when (resp.method) {
        HttpMethod.POST -> convertResponseToJson(resp)
        else -> convertResponseToPlainText(resp)
    }
}

private fun convertResponseToJson(resp: ResponseContext): Observable<ResponseContext>? {
    return resp.bufferObservable.map<ResponseContext> {
        try {
            val jsonObject = it.toJsonObject()
            resp.body = JsonObject().put(resp.key, jsonObject).encodePrettily()
            LOGGER.debug("converted json ${resp.body}")
        } catch (e: Exception) {
            resp.body = it.toString(Charsets.UTF_8.name())
        }
        resp
    }.doOnError { convertResponseToPlainText(resp) }
}

private fun convertResponseToPlainText(resp: ResponseContext): Observable<ResponseContext>? {
    return resp.bufferObservable.map {
        resp.body = it.toString(Charsets.UTF_8.name())
        LOGGER.debug("converted text ${resp.body}")
        resp
    }
}

private fun parseRequestStrategy(routingContext: RoutingContext): List<KalforRequest> {
    val newHeaders = filterHeaders(routingContext)

    val proxyHeaders = convertMultiMapToProxyHeaders(newHeaders)

    return when (routingContext.request().method()) {
        HttpMethod.POST -> routingContext.bodyAsJsonArray
                .filter { it is JsonObject }
                .map { it as JsonObject }
                .map { addClientRequestHeaders(it, proxyHeaders) }
                .map { Json.decodeValue(it.encode(), KalforRequest::class.java) }
        HttpMethod.GET -> routingContext.request().params().getAll("c")
                .map { makeKalforRequest(it, proxyHeaders) }
        else -> emptyList()
    }
}

private fun filterHeaders(routingContext: RoutingContext): MultiMap {
    val headers = routingContext.request().headers()
    return routingContext.request().headers().names()
            .filter { it.startsWith("x-", true) }
            .fold(MultiMap.caseInsensitiveMultiMap()) { map, header ->
                map.add(header, headers.get(header))
            }
}

private fun makeKalforRequest(it: String?, proxyHeaders: List<KalforProxyHeader>): KalforRequest {
    val url = URI(URLDecoder.decode(it, Charsets.UTF_8.name()))
    return KalforRequest(
            "${url.scheme}://${url.host}:${url.port}",
            proxyHeaders,
            listOf(KalforProxyRequest("unused", url.rawPath)),
            HttpMethod.GET)
}

private fun addClientRequestHeaders(it: JsonObject, proxyHeaders: List<KalforProxyHeader>): JsonObject {
    it.getJsonArray("headers", JsonArray())
            .addAll(JsonArray(proxyHeaders.map {
                JsonObject(Json.encode(it))
            }))
    return it
}

private fun convertMultiMapToProxyHeaders(newHeaders: MultiMap) =
        newHeaders.names().map { KalforProxyHeader(it, newHeaders.get(it)) }

fun respondToClient(
        serverRequest: HttpServerRequest,
        serverResponse: HttpServerResponse
): (String?) -> Unit = {
    LOGGER.debug("------- writing response: $it")
    serverRequest.getHeader(HttpHeaders.CONTENT_TYPE).let {
        serverResponse.putHeader(HttpHeaders.CONTENT_TYPE, it)
    }

    serverResponse.end(it)
}

private fun HttpServerResponse.putHeader(header: CharSequence, value: String?) {
    putHeader(header.toString(), value)
}

private fun HttpServerRequest.getHeader(header: CharSequence): String? =
        getHeader(header.toString())

fun makeHttpGetRequest(url: String, headers: List<KalforProxyHeader>?, vertx: Vertx): Observable<HttpClientResponse> {
    val httpClient = getHttpClient(vertx)
    val uri = URI(url)
    val port = if (uri.port == -1) 80 else uri.port
    val multiMapHeaders = headers
            ?.fold(MultiMap.caseInsensitiveMultiMap(),
                    { m, h -> m.add(h.name, h.value) })
    return Observable.unsafeCreate<HttpClientResponse> { subscriber ->
        val req = httpClient.get(port, uri.host, uri.path)
        req.headers()
                .addAll(multiMapHeaders)
                .add("User-Agent", "kalfor-3.2.1")
        val resp = req
                .setFollowRedirects(true)
                .toObservable()
        resp.subscribe(subscriber)
        req.end()
    }.doOnError { LOGGER.error(it.message, it) }
}

private fun executeRequest(vertx: Vertx): (KalforRequest) -> List<ResponseContext> {
    return { (proxyBaseUrl, headers, proxyRequests, type) ->
        proxyRequests.map {
            val response = makeHttpGetRequest("$proxyBaseUrl${it.path}", headers, vertx)
            ResponseContext(
                    type,
                    it.key,
                    response.flatMap { it.toObservable() }
            )
        }
    }
}

private val getHttpClient: (Vertx) -> HttpClient = { v: Vertx ->
    val httpClientOptions = HttpClientOptions()
            .setTrustAll(true)
            .setVerifyHost(false)
            .setMaxRedirects(10)
            .setMaxChunkSize(10240)
    LOGGER.info("created HttpClient")
    v.createHttpClient(httpClientOptions)
}.memoize()

