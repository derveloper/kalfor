package cc.vileda.kalfor.handler

import io.vertx.rxjava.core.AbstractVerticle
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.handler.BodyHandler
import io.vertx.rxjava.ext.web.handler.CorsHandler


internal class KalforTestVerticle(private val listenPort: Int) : AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()

        val router = Router.router(vertx)
        router.route().handler(CorsHandler.create("*").allowedHeader("authorization"))
        router.route().handler(BodyHandler.create())

        router.route().handler(SchemaValidationHandler())
        router.route().handler(CombineHandler(vertx))

        httpServer.requestHandler({ router.accept(it) }).listen(listenPort)
    }
}
