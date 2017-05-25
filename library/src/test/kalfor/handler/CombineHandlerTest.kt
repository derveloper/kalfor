package cc.vileda.kalfor.handler

import cc.vileda.kalfor.mock.KalforTestVerticle
import cc.vileda.kalfor.mock.RestApi2Mock
import cc.vileda.kalfor.mock.RestApiMock
import cc.vileda.kalfor.mock.StaticFileServerMock
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.Header
import io.vertx.core.json.Json
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
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.net.URLEncoder
import java.util.*

@RunWith(VertxUnitRunner::class)
class CombineHandlerTest {
    @Test
    fun staticFileServerMockShouldRespond() {
        given()
                .header("x-foo", "bar")
        .`when`()
            .get("http://localhost:$staticFileServerMockPort/test1.css")
        .then()
            .body(containsString("#test1 { margin: 0 auto; }"))

        given()
            .header("x-foo", "bar")
        .`when`()
            .get("http://localhost:$staticFileServerMockPort/test2.css")
        .then()
            .body(containsString("#test2 { margin: 0 auto; }"))
    }

    @Test
    fun restApiMockShouldRespond() {
        given()
            .header("x-foo", "bar")
        .`when`()
            .get("http://localhost:$restApiMockPort/test")
         .then()
            .body(containsString(JsonObject().put("1foo", "1bar").encodePrettily()))
    }

    @Test
    fun combineHandlerShouldCombineViaPost() {
        val given = JsonArray(listOf(KalforRequest(
                "http://localhost:$restApiMockPort",
                listOf(Kalfor2ProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2"),
                        KalforProxyRequest("3thirdKey", "/test")))))
                .encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .put("3thirdKey", JsonObject().put("1foo", "1bar"))
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
    fun combineHandlerShouldCombineRedirectedViaPost() {
        val given = JsonArray(listOf(KalforRequest(
                "http://localhost:$restApiMockPort",
                listOf(Kalfor2ProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2"),
                        KalforProxyRequest("3thirdKey", "/testrdr")))))
                .encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .put("3thirdKey", JsonObject().put("1foo", "1bar"))
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
    fun combineHandlerShouldCombineInvalidJsonViaPost() {
        val given = JsonArray(listOf(KalforRequest(
                "http://localhost:$restApiMockPort",
                listOf(Kalfor2ProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2"),
                        KalforProxyRequest("3thirdKey", "/testbroken")))))
                .encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .put("3thirdKey", "{Broken!")
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
                listOf(Kalfor2ProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test2")))

        val req2 = KalforRequest(
                "http://localhost:$restApi2MockPort",
                listOf(Kalfor2ProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("3thirdKey", "/test3"),
                        KalforProxyRequest("4fourthKey", "/test4")))

        val given = JsonArray(listOf(req1, req2)).encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("2foo", "2bar"))
                .put("3thirdKey", JsonObject().put("1foo", "1bar"))
                .put("4fourthKey", JsonObject().put("2foo", "2bar")
                        .put("a3foo", "a3bar"))
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
    fun combineHandlerShouldCombineViaPostWOHeaders() {
        val req1 = KalforRequest(
                "http://localhost:$restApiMockPort",
                emptyList(),
                Arrays.asList(
                        KalforProxyRequest("1firstKey", "/test"),
                        KalforProxyRequest("2secondKey", "/test")))

        val request = JsonObject(Json.encode(req1))
        request.remove("headers")
        val given = JsonArray(listOf(request)).encodePrettily()

        val expected = JsonObject()
                .put("1firstKey", JsonObject().put("1foo", "1bar"))
                .put("2secondKey", JsonObject().put("1foo", "1bar"))
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
            RxHelper.deployVerticle(vertx, RestApiMock(restApiMockPort))
                    .mergeWith(RxHelper.deployVerticle(vertx, RestApi2Mock(restApi2MockPort)))
                    .mergeWith(RxHelper.deployVerticle(vertx, StaticFileServerMock(staticFileServerMockPort)))
                    .mergeWith(RxHelper.deployVerticle(vertx, KalforTestVerticle(kalforPort)))
                    .reduce("") { a, b -> "$a, $b" }
                    .subscribe {
                        LOGGER.info("deployed Kalfor" + it)
                        async.complete()
                    }
        }
    }
}