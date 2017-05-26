package cc.vileda.kalfor2

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.getAs
import com.github.salomonbrys.kotson.jsonObject
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.funktionale.tries.Try
import org.funktionale.utils.identity
import java.nio.charset.Charset
import java.util.stream.Collectors
import java.util.stream.Stream

data class
KalforResponse(val name: String, val statusCode: Int, val headers: List<KalforProxyHeader>, val body: String)

data class KalforRequestException(
        val statusCode: Int = -1,
        override val message: String = ""
) : RuntimeException()

typealias KalforFetcher =
(request: KalforProxyRequest, headers: List<KalforProxyHeader>) -> Try<KalforResponse>

fun kalfor(requests: List<KalforRequest>): List<KalforResponse> {
    return kalfor(requests, httpFetcher)
}

fun kalfor(requests: List<KalforRequest>, fetcher: KalforFetcher): List<KalforResponse> {
    return requests.parallelStream()
            .flatMap { mapRequestToResponses(it, fetcher) }
            .collect(Collectors.toList())
}

private fun mapRequestToResponses(request: KalforRequest, fetcher: KalforFetcher): Stream<KalforResponse> {
    return request.proxyRequests.parallelStream()
            .map {
                mapProxyRequestToResponse(
                        it.copy(path = "${request.proxyBaseUrl}${it.path}"),
                        request.headers,
                        fetcher)
            }
}

private fun mapProxyRequestToResponse(it: KalforProxyRequest,
                                      headers: List<KalforProxyHeader>,
                                      fetcher: KalforFetcher): KalforResponse {
    return fetcher(it, headers).toEither()
            .fold({ e ->
                val msg = e.message ?: "Unknown error"
                when (e) {
                    is KalforRequestException -> {
                        KalforResponse(it.key, e.statusCode, emptyList(), jsonObject("error" to msg).toString())
                    }
                    else -> KalforResponse(it.key, 500, emptyList(), jsonObject("error" to msg).toString())
                }
            }, identity())
}

val httpFetcher: KalforFetcher = { (key, path), _ ->
    val (_, response, result) = path.httpGet().responseString()
    when (response.httpStatusCode) {
        in 200..300 -> {
            Try.Success(KalforResponse(key,
                    response.httpStatusCode,
                    headersFrom(response.httpResponseHeaders),
                    result.getAs() ?: ""))
        }
        in 400..999 -> {
            Try.Success(KalforResponse(key,
                    response.httpStatusCode,
                    headersFrom(response.httpResponseHeaders),
                    response.data.toString(Charset.defaultCharset())))
        }
        else -> {
            Try.Failure(KalforRequestException(response.httpStatusCode,
                    result.component2()?.exception?.message ?: "Unknown error"))
        }
    }
}

private fun headersFrom(httpResponseHeaders: Map<String, List<String>>): List<KalforProxyHeader> {
    return httpResponseHeaders.filterKeys { isNotBlank(it) }.flatMap {
        val name = it.key
        it.value.map {
            KalforProxyHeader(name, it)
        }
    }
}

