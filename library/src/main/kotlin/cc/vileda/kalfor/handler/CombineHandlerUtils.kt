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

fun aggregateResponse() = { t1: String, t2: ResponseContext ->
    if (t2.method == HttpMethod.POST) {
        val body = JsonObject(t2.body)
        val jsonObject = if (t1 == "") JsonObject() else JsonObject(t1)
        val key = body.fieldNames().first()
        JsonObject(TreeMap(jsonObject.put(key, body.getJsonObject(key)).map)).encodePrettily()
    } else {
        t1.plus("\n${t2.body}")
    }
}

fun convertResponseStrategy(resp: ResponseContext): Observable<ResponseContext>? {
    return if (resp.method == HttpMethod.POST) {
        resp.bufferObservable.map {
            val jsonObject = JsonObject(it.toString(Charsets.UTF_8.name()))
            resp.body = JsonObject().put(resp.key, jsonObject).encodePrettily()
            println("converted json ${resp.body}")
            resp
        }
    } else {
        resp.bufferObservable.map {
            resp.body = it.toString(Charsets.UTF_8.name())
            println("converted text ${resp.body}")
            resp
        }
    }
}

private fun parseRequestStrategy(routingContext: RoutingContext): List<KalforRequest> {
    val newHeaders = MultiMap.caseInsensitiveMultiMap()
    val headers = routingContext.request().headers()
    headers.names().forEach {
        if (it.startsWith("x-", true)) {
            newHeaders.add(it, headers.get(it))
        }
    }

    val proxyHeaders = convertMultiMapToProxyHeaders(newHeaders)

    if (routingContext.request().method() == HttpMethod.GET) {
        return routingContext.request().params().getAll("c")
                .map {
                    val url = URI(URLDecoder.decode(it, Charsets.UTF_8.name()))
                    KalforRequest("${url.scheme}://${url.host}:${url.port}", proxyHeaders, listOf(KalforProxyRequest("unused", url.rawPath)), HttpMethod.GET)
                }
    }
    else if (routingContext.request().method() == HttpMethod.POST) {
        return routingContext.bodyAsJsonArray
                .filter { it is JsonObject }
                .map { it as JsonObject }
                .map {
                    it.getJsonArray("headers").addAll(JsonArray(proxyHeaders.map { JsonObject(Json.encode(it)) }))
                    it
                }
                .map { Json.decodeValue(it.encode(), KalforRequest::class.java) }
    }

    return emptyList()
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

fun makeHttpGetRequest(url: String, headers: List<KalforProxyHeader>, vertx: Vertx) : Observable<Buffer> {
    val httpClient = getHttpClient(vertx)
    val uri = URI(url)
    val port = if (uri.port == -1) 80 else uri.port
    val multiMapHeaders = headers.fold(MultiMap.caseInsensitiveMultiMap(), { m, h ->
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

