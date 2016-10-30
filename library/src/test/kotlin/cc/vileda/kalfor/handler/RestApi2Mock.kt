package cc.vileda.kalfor.handler

import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router


internal class RestApi2Mock(private val port: Int) : AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.route("/test3").handler { routingContext ->
            val headers = routingContext.request().headers()
            LOGGER.info(Json.encodePrettily(headers.names()))
            LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            routingContext.response().putHeader("content-type", "application/json").end(JsonObject().put("1foo", "1bar").encodePrettily())
        }

        router.route("/test4").handler { routingContext ->
            val headers = routingContext.request().headers()
            LOGGER.info(Json.encodePrettily(headers.names()))
            LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            routingContext.response().putHeader("content-type", "application/json").end(JsonObject().put("2foo", "2bar").put("a3foo", "a3bar").encodePrettily())
        }

        httpServer.requestHandler({ router.accept(it) }).listen(port)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RestApiMock::class.java)
    }
}
