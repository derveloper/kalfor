package cc.vileda.kalfor.handler

import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.MultiMap
import io.vertx.rxjava.core.RxHelper
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.buffer.Buffer
import io.vertx.rxjava.core.http.HttpClient
import io.vertx.rxjava.core.http.HttpServerRequest
import io.vertx.rxjava.core.http.HttpServerResponse
import io.vertx.rxjava.ext.web.RoutingContext
import rx.Observable
import java.net.URI
import java.net.URLDecoder
import java.util.*

private val LOGGER = LoggerFactory.getLogger(CombineHandler::class.java)


fun parseRequest(routingContext: RoutingContext, vertx: Vertx): Observable<ResponseContext> {
    return Observable.from(parseRequestStrategy(routingContext)
            .flatMap(executeRequest(vertx)))
            .doOnEach(::println)
            .doOnError { it.printStackTrace() }
}

fun aggregateResponseStrategy() = { last: String, ctx: ResponseContext ->
    when (ctx.method) {
        HttpMethod.POST -> aggregateJsonResponse(last, ctx)
        else -> aggregatePlainTextResponse(last, ctx)
    }
}

private fun aggregatePlainTextResponse(last: String, ctx: ResponseContext) = last.plus("\n${ctx.body}")

private fun aggregateJsonResponse(last: String, ctx: ResponseContext): String {
    val body = JsonObject(ctx.body)
    val jsonObject = if (last == "") JsonObject() else JsonObject(last)
    val key = body.fieldNames().first()
    return JsonObject(TreeMap(jsonObject.put(key, body.getJsonObject(key)).map)).encodePrettily()
}

fun convertResponseStrategy(resp: ResponseContext): Observable<ResponseContext>? {
    return when (resp.method) {
        HttpMethod.POST -> convertResponseToJson(resp)
        else -> convertResponseToPlainText(resp)
    }
}

private fun convertResponseToJson(resp: ResponseContext): Observable<ResponseContext>? {
    return resp.bufferObservable.map {
        val jsonObject = JsonObject(it.toString(Charsets.UTF_8.name()))
        resp.body = JsonObject().put(resp.key, jsonObject).encodePrettily()
        println("converted json ${resp.body}")
        resp
    }
}

private fun convertResponseToPlainText(resp: ResponseContext): Observable<ResponseContext>? {
    return resp.bufferObservable.map {
        resp.body = it.toString(Charsets.UTF_8.name())
        println("converted text ${resp.body}")
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
    val newHeaders = MultiMap.caseInsensitiveMultiMap()
    val headers = routingContext.request().headers()
    headers.names()
            .filter { it.startsWith("x-", true) }
            .forEach { newHeaders.add(it, headers.get(it)) }
    return newHeaders
}

private fun makeKalforRequest(it: String?, proxyHeaders: List<KalforProxyHeader>): KalforRequest {
    val url = URI(URLDecoder.decode(it, Charsets.UTF_8.name()))
    return KalforRequest("${url.scheme}://${url.host}:${url.port}", proxyHeaders, listOf(KalforProxyRequest("unused", url.rawPath)), HttpMethod.GET)
}

private fun addClientRequestHeaders(it: JsonObject, proxyHeaders: List<KalforProxyHeader>): JsonObject {
    it.getJsonArray("headers", JsonArray()).addAll(JsonArray(proxyHeaders.map { JsonObject(Json.encode(it)) }))
    return it
}

private fun convertMultiMapToProxyHeaders(newHeaders: MultiMap) = newHeaders.names().map { KalforProxyHeader(it, newHeaders.get(it)) }

fun respondToClient(
        serverRequest: HttpServerRequest,
        serverResponse: HttpServerResponse
): (String?) -> Unit = {
    LOGGER.info("------- Swriting response: $it")
    if (serverRequest.getHeader("content-type") != null) {
        serverResponse.putHeader("content-type", serverRequest.getHeader("content-type"))
    }

    serverResponse.end(it)
}

fun makeHttpGetRequest(url: String, headers: List<KalforProxyHeader>?, vertx: Vertx) : Observable<Buffer> {
    val httpClient = getHttpClient(vertx)
    val uri = URI(url)
    val port = if (uri.port == -1) 80 else uri.port
    val multiMapHeaders = headers?.fold(MultiMap.caseInsensitiveMultiMap(), { m, h ->
        m.add(h.name, h.value)
    })
    return RxHelper.get(httpClient, port, uri.host, uri.path, multiMapHeaders)
            .doOnError { it.printStackTrace() }
            .flatMap{ it.toObservable() }
}

private fun executeRequest(vertx: Vertx): (KalforRequest) -> List<ResponseContext> {
    return { request ->
        request.proxyRequests.map {
            val bufferObservable = makeHttpGetRequest("${request.proxyBaseUrl}${it.path}", request.headers, vertx)
            ResponseContext(request.type, it.key, bufferObservable)
        }
    }
}

private fun getHttpClient(vertx: Vertx): HttpClient {
    val httpClientOptions = HttpClientOptions()
            .setTrustAll(true)
            .setVerifyHost(false)
    return vertx.createHttpClient(httpClientOptions)
}

