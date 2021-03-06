package cc.vileda.kalfor

import com.github.salomonbrys.kotson.*
import com.google.gson.Gson
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.funktionale.tries.Try
import org.junit.Test

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
        val result = kalforPost(listOf(
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
                responseMock1(),
                response404Mock()
        )
        val result = kalforPost(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:$port",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/"),
                                KalforProxyRequest("res2", "/404")
                        )
                )
        ))
        assertEquals(expected, result)
    }

    @Test
    fun kalfor_should_map_get_requests_with_http_fetcher() {
        val port = mockServer()
        val expected = "test-1\ntest-2"
        val result = kalforGet(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:$port",
                        proxyRequests = listOf(
                                KalforProxyRequest("res-text", "/res-text"),
                                KalforProxyRequest("res-text2", "/res-text2")
                        )
                )
        ))
        assertEquals(expected, result)
    }

    @Test
    fun kalfor_should_map_redirected_requests_with_http_fetcher() {
        val port = mockServer()
        val expected = listOf(
                responseMock1(),
                response404Mock()
        )
        val result = kalforPost(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:$port",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/"),
                                KalforProxyRequest("res2", "/301")
                        )
                )
        ))
        assertEquals(expected, result)
    }

    @Test
    fun kalfor_should_map_error_requests_with_http_fetcher() {
        val result = kalforPost(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:45874",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/")
                        )
                )
        ))
        assertEquals(result.size, 1)
        assertTrue(result[0].body.contains("Connection refused"))
    }

    @Test
    fun validates_valid_request() {
        val request = listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:45874",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/")
                        )
                )
        )
        val actual = validateSchema(Gson().toJson(request))
        assertEquals(actual, Try.Success(true))
    }

    @Test
    fun validate_fail_on_invalid_request() {
        val request = Gson().typedToJsonTree(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:45874",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/")
                        )
                )
        )).asJsonArray
        request[0]["proxyBaseUrl"] = null
        val actual = validateSchema(request.toString())
        assertTrue(actual.isFailure())
        assertEquals(actual.failed().get().message, "instance type (null) " +
                "does not match any allowed primitive type " +
                "(allowed: [\"string\"])")
    }

    private fun response404Mock(): KalforResponse {
        return KalforResponse("res2", 404,
                listOf(KalforProxyHeader("Connection", "keep-alive"),
                        KalforProxyHeader("Content-Length", "78"),
                        KalforProxyHeader("Content-Type", "text/html; charset=UTF-8")
                ),
                "<H1>404 Not Found</H1><P>Cannot find resource with the requested URI: /404</P>")
    }

    private fun responseMock1(): KalforResponse {
        return KalforResponse("res1", 200,
                listOf(KalforProxyHeader("Connection", "keep-alive"),
                        KalforProxyHeader("Content-Length", "13"),
                        KalforProxyHeader("Content-Type", "application/json")
                ),
                responseMock)
    }
}

