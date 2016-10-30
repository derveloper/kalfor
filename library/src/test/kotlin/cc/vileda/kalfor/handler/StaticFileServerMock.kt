package cc.vileda.kalfor.handler

import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router


internal class StaticFileServerMock(private val port: Int) : AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.route("/test1.css").handler { routingContext ->
            val headers = routingContext.request().headers()
            LOGGER.info(Json.encodePrettily(headers.names()))
            LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            LOGGER.info(routingContext.request().absoluteURI())
            routingContext.response().end("#test1 { margin: 0 auto; }")
        }

        router.route("/test2.css").handler { routingContext ->
            val headers = routingContext.request().headers()
            LOGGER.info(Json.encodePrettily(headers.names()))
            LOGGER.info(Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            LOGGER.info(routingContext.request().absoluteURI())
            routingContext.response().end("#test2 { margin: 0 auto; }")
        }

        httpServer.requestHandler({ router.accept(it) }).listen(port)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StaticFileServerMock::class.java)
    }
}
