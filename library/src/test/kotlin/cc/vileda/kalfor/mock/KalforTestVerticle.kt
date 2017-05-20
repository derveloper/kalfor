package cc.vileda.kalfor.mock

import cc.vileda.kalfor.handler.CombineHandler
import cc.vileda.kalfor.handler.SchemaValidationHandler
import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.rxjava.ext.web.handler.CorsHandler


internal class KalforTestVerticle(private val listenPort: Int) : AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()

        val router = io.vertx.rxjava.ext.web.Router.router(vertx)
        router.route().handler(CorsHandler.create("*").allowedHeader("authorization"))
        router.route().handler(BodyHandler.create())

        router.post().handler(SchemaValidationHandler())
        router.route().handler(CombineHandler(vertx))

        httpServer.requestHandler({ router.accept(it) }).listen(listenPort)
    }
}
