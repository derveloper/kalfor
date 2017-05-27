package cc.vileda.kalfor

import com.github.kittinunf.fuel.httpGet
import org.junit.Test
import java.nio.charset.Charset
import kotlin.test.assertTrue

internal class KalforIT {
    @Test
    fun kalfor_should_map_requests_with_http_fetcher() {
        val mockServerPort = mockServer2()
        val mockKalforPort = mockKalforService()

        val expected = "test-1"

        val request = ("http://localhost:$mockKalforPort/combine?c=" +
                "http://localhost:$mockServerPort/res-text").httpGet()
        val result = request.response().third.get()
                .toString(Charset.defaultCharset())

        assertTrue(expected == result)
    }
}

