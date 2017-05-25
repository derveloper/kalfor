package cc.vileda.kalfor.mock

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router


internal class RestApiMock(private val port: Int) : AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()
        val router = Router.router(vertx)

        router.route("/test").handler { routingContext ->
            val headers = routingContext.request().headers()
            LOGGER.info(Json.encodePrettily(headers.names()))
            LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            routingContext.response().putHeader("content-type", "application/json").end(JsonObject().put("1foo", "1bar").encodePrettily())
        }

        router.route("/test2").handler { routingContext ->
            if (routingContext.request().getHeader("x-foo") == null) {
                routingContext.response().setStatusCode(500).end("missing header x-foo")
            } else {
                val headers = routingContext.request().headers()
                LOGGER.info(Json.encodePrettily(headers.names()))
                LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
                routingContext.response().putHeader("content-type", "application/json").end(JsonObject().put("2foo", "2bar").encodePrettily())
            }
        }

        router.route("/testrdr").handler { routingContext ->
            routingContext.response()
                    .putHeader("Location", "/test")
                    .setStatusCode(301)
                    .end("Redirect!")
        }

        router.route("/testbroken").handler { routingContext ->
            routingContext.response()
                    .end("{Broken!")
        }

        httpServer.requestHandler({ router.accept(it) }).listen(port)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RestApiMock::class.java)
    }
}
