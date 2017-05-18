package cc.vileda.kalfor.mock

import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.rxjava.ext.web.handler.CorsHandler


internal class KalforTestVerticle(private val listenPort: Int) : io.vertx.rxjava.core.AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()

        val router = io.vertx.rxjava.ext.web.Router.router(vertx)
        router.route().handler(io.vertx.rxjava.ext.web.handler.CorsHandler.create("*").allowedHeader("authorization"))
        router.route().handler(io.vertx.rxjava.ext.web.handler.BodyHandler.create())

        router.post().handler(cc.vileda.kalfor.handler.SchemaValidationHandler())
        router.route().handler(cc.vileda.kalfor.handler.CombineHandler(vertx))

        httpServer.requestHandler({ router.accept(it) }).listen(listenPort)
    }
}
