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
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.ServerSocket
import java.util.*


@RunWith(VertxUnitRunner::class)
class CombineHandlerTest
constructor() {
    private val kalforPort = randomPort
    private val remotePort = randomPort

    private val randomPort: Int
        get() {
            val socket = ServerSocket(0)
            val port = socket.localPort
            socket.close()

            return port
        }

    @Before
    fun setUp(context: TestContext) {
        val vertx = Vertx.vertx()
        val async = context.async()
        val deployVerticle = RxHelper.deployVerticle(vertx, RestApiMock(remotePort))
        val deployVerticle2 = RxHelper.deployVerticle(
                vertx,
                KalforTestVerticle(kalforPort))
        deployVerticle.subscribe { s ->
            println("deployed Api Mock " + s)
            deployVerticle2.subscribe { s1 ->
                println("deployed Kalfor " + s1)
                async.complete()
            }
        }
    }

    @Test
    fun restApiMockShouldRespond() {
        get("http://localhost:$remotePort/test").then().body(containsString(JsonObject().put("foo", "bar").encodePrettily()))
    }

    @Test
    fun combineHandlerShouldCombine() {
        val given = JsonArray(listOf(KalforRequest(
                "http://localhost:" + remotePort,
                listOf(KalforProxyHeader("x-foo", "bar")),
                Arrays.asList(
                        KalforProxyRequest("firstKey", "/test"),
                        KalforProxyRequest("secondKey", "/test"))))).encodePrettily()

        val expected = JsonObject().put("firstKey", JsonObject().put("foo", "bar")).put("secondKey", JsonObject().put("foo", "bar")).encodePrettily()
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
                        "  \"error\" : [ \"object has missing required properties ([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\", \"object has missing required " +
                        "properties ([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\" ]\n" +
                        "}"))
    }
}
