package cc.vileda.kalfor2

import com.github.salomonbrys.kotson.jsonObject
import org.funktionale.tries.Try
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class KalforKtTest {
    val responseMock = jsonObject("foo" to "bar").toString()
    val failedResponseMock = jsonObject("error" to "internal server error").toString()
    val headers = listOf(KalforProxyHeader("Content-Type", "application/json"))

    val fetcher: KalforFetcher = { (key, path), _ ->
        when (path) {
            "https://api.sipgate.com/error" -> Try.Failure(RuntimeException("internal server error"))
            else -> Try.Success(KalforResponse(key, 200, headers, responseMock))
        }
    }

    @Test
    fun kalfor_should_map_requests() {
        val expected = listOf(
                KalforResponse("sipgateUrls", 200, headers, responseMock),
                KalforResponse("sipgateTranslations", 200, headers, responseMock),
                KalforResponse("error", 500, emptyList(), failedResponseMock)
        )
        val result = kalfor(listOf(
                KalforRequest(
                        proxyBaseUrl = "https://api.sipgate.com",
                        proxyRequests = listOf(
                                KalforProxyRequest("sipgateUrls", "/v1"),
                                KalforProxyRequest("sipgateTranslations", "/v1/translations/de_DE"),
                                KalforProxyRequest("error", "/error")
                        )
                )
        ), fetcher)
        assertEquals(expected, result)
    }

    @Test
    fun kalfor_should_map_requests_with_http_fetcher() {
        val port = mockServer()
        val expected = listOf(
                KalforResponse("res1", 200,
                        listOf(
                                KalforProxyHeader("Connection", "keep-alive"),
                                KalforProxyHeader("Content-Length", "13"),
                                KalforProxyHeader("Content-Type", "application/json")
                        ),
                        responseMock),
                KalforResponse("res2", 404,
                        listOf(
                                KalforProxyHeader("Connection", "keep-alive"),
                                KalforProxyHeader("Content-Length", "78"),
                                KalforProxyHeader("Content-Type", "text/html; charset=UTF-8")
                        ),
                        "<H1>404 Not Found</H1><P>Cannot find resource with the requested URI: /404</P>")
        )
        val result = kalfor(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:$port",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/"),
                                KalforProxyRequest("res2", "/404")
                        )
                )
        ), httpFetcher)
        assertEquals(expected, result)
    }
}