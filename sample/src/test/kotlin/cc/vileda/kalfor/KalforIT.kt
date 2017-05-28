package cc.vileda.kalfor

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import com.github.salomonbrys.kotson.typedToJsonTree
import com.google.gson.Gson
import junit.framework.TestCase
import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.assertTrue

internal class KalforIT {
    @Test
    fun kalfor_should_map_get_requests() {
        val mockServerPort = mockServer2()
        val mockKalforPort = mockKalforService()

        val expected = "test-1"

        val request = ("http://localhost:$mockKalforPort/combine?c=" +
                "http://localhost:$mockServerPort/res-text").httpGet()
        val result = request.response().third.get()
                .toString(Charset.defaultCharset())

        assertTrue(expected == result)
    }

    @Test
    fun kalfor_should_post_get_requests() {
        val mockServerPort = mockServer2()
        val mockKalforPort = mockKalforService()

        val expected = listOf(
                responseMock1(),
                response404Mock()
        )
        val body = Gson().toJson(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:$mockServerPort",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/"),
                                KalforProxyRequest("res2", "/404")
                        )
                )
        ))
        val request = ("http://localhost:$mockKalforPort/combine").httpPost()
        request.request.body(body, Charset.defaultCharset())
        val result = Gson().fromJson<List<KalforResponse>>(request.response().third.get()
                .toString(Charset.defaultCharset()))

        assertTrue(expected == result)
    }

    @Test
    fun validate_fail_on_invalid_request() {
        val mockKalforPort = mockKalforService()

        val body = Gson().typedToJsonTree(listOf(
                KalforRequest(
                        proxyBaseUrl = "http://localhost:45874",
                        proxyRequests = listOf(
                                KalforProxyRequest("res1", "/")
                        )
                )
        )).asJsonArray
        body[0]["proxyBaseUrl"] = null

        val request = ("http://localhost:$mockKalforPort/combine").httpPost()
        request.request.body(body.toString(), Charset.defaultCharset())

        val result = request.response().third.get()
                .toString(Charset.defaultCharset())

        TestCase.assertEquals(result, "instance type (null) " +
                "does not match any allowed primitive type " +
                "(allowed: [\"string\"])")
    }

    val responseMock = jsonObject("foo" to "bar").toString()

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

