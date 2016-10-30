package cc.vileda.kalfor.handler

import com.jayway.restassured.RestAssured
import com.jayway.restassured.RestAssured.get
import com.jayway.restassured.response.Header
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.rxjava.core.RxHelper
import io.vertx.rxjava.core.Vertx
import org.apache.http.HttpStatus
import org.apache.http.entity.ContentType
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.net.URLEncoder
import java.util.*

@RunWith(VertxUnitRunner::class)
class CombineHandlerTest {
    @Test
    fun staticFileServerMockShouldRespond() {
        get("http://localhost:$staticFileServerMockPort/test1.css")
                .then()
                .body(containsString("#test1 { margin: 0 auto; }"))

        get("http://localhost:$staticFileServerMockPort/test2.css")
                .then()
                .body(containsString("#test2 { margin: 0 auto; }"))
    }

    @Test
    fun restApiMockShouldRespond() {
        get("http://localhost:$restApiMockPort/test")
                .then()
                .body(containsString(JsonObject().put("1foo", "1bar").encodePrettily()))
    }

    @Test
    fun combineHandlerShouldCombineViaPost() {
        val given = JsonArray(listOf(KalforRequest(
                "http://localhost:$restApiMockPort",
                listOf(KalforProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2"))))).encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .encodePrettily()

        RestAssured
                .given()
                .body(given)
                .header(Header("Content-Type", "application/json"))
                .`when`()
                .post("http://localhost:$kalforPort/combine")
                .then()
                .body(Matchers.containsString(expected))
    }

    @Test
    fun combineHandlerShouldCombineMultipleViaPost() {
        val req1 = KalforRequest(
                "http://localhost:$restApiMockPort",
                listOf(KalforProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2")))

        val req2 = KalforRequest(
                "http://localhost:$restApi2MockPort",
                listOf(KalforProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("3thirdKey", "/test3"),
                        KalforProxyRequest("4fourthKey", "/test4")))

        val given = JsonArray(listOf(req1, req2)).encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .put("3thirdKey", JsonObject().put("1foo", "1bar"))
                .put("4fourthKey", JsonObject().put("2foo", "2bar").put("a3foo", "a3bar"))
                .encodePrettily()

        RestAssured
                .given()
                .body(given)
                .header(Header("Content-Type", "application/json"))
                .`when`()
                .post("http://localhost:$kalforPort/combine")
                .then()
                .body(Matchers.containsString(expected))
    }

    @Test
    fun combineHandlerShouldCombineViaGet() {
        val given = listOf(
                "http://localhost:$staticFileServerMockPort/test1.css",
                "http://localhost:$staticFileServerMockPort/test2.css"
        ).map { URLEncoder.encode(it, Charsets.UTF_8.name()) }

        val expected = """#test1 { margin: 0 auto; }
#test2 { margin: 0 auto; }"""

        RestAssured
                .given()
                .param("c", given)
                .header("x-foo", "bar")
                .header("content-type", ContentType.TEXT_PLAIN)
                .`when`()
                .get("http://localhost:$kalforPort/combine")
                .then()
                .body(Matchers.containsString(expected))
    }

    @Test
    fun combineHandlerShouldRespondWithBadRequestOnInvalidRequest() {
        val given = JsonArray(Arrays.asList(
                JsonObject().put("foo", "bar"),
                JsonObject().put("baz", JsonObject().put("bar", "foo")))).encodePrettily()

        RestAssured
                .given()
                .body(given)
                .header(Header("Content-Type", "application/json"))
                .`when`()
                .post("http://localhost:$kalforPort/combine")
                .then()
                .statusCode(Matchers.`is`(HttpStatus.SC_BAD_REQUEST))
                .body(Matchers.containsString("{\n" +
                        "  \"error\" : [ \"object has missing required properties " +
                        "([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\", " +
                        "\"object has missing required " +
                        "properties ([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\" ]\n" +
                        "}"))
    }

    companion object {
        private val kalforPort = randomPort
        private val restApiMockPort = randomPort
        private val restApi2MockPort = randomPort
        private val staticFileServerMockPort = randomPort

        private val randomPort: Int
            get() {
                val socket = ServerSocket(0)
                val port = socket.localPort
                socket.close()

                return port
            }

        @BeforeClass
        @JvmStatic
        fun setUp(context: TestContext) {
            val vertx = Vertx.vertx()
            val async = context.async()
            val restApiMockVerticle = RxHelper.deployVerticle(vertx, RestApiMock(restApiMockPort))
            val restApi2MockVerticle = RxHelper.deployVerticle(vertx, RestApi2Mock(restApi2MockPort))
            val staticFileServerMockVerticle = RxHelper.deployVerticle(vertx, StaticFileServerMock(staticFileServerMockPort))
            val kalforVerticle = RxHelper.deployVerticle(vertx, KalforTestVerticle(kalforPort))
    
            restApiMockVerticle
                    .mergeWith(restApi2MockVerticle)
                    .mergeWith(staticFileServerMockVerticle)
                    .mergeWith(kalforVerticle)
                    .subscribe {
                        println("deployed Kalfor " + it)
                        async.complete()
                    }
        }
    }
}