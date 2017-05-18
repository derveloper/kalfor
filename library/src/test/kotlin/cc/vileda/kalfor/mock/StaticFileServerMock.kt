package cc.vileda.kalfor.mock


internal class StaticFileServerMock(private val port: Int) : io.vertx.rxjava.core.AbstractVerticle() {
    override fun start() {
        val httpServer = vertx.createHttpServer()
        val router = io.vertx.rxjava.ext.web.Router.router(vertx)
        router.route().handler { routingContext ->
            if (routingContext.request().getHeader("x-foo") == null) {
                routingContext.response().setStatusCode(500).end("missing header x-foo")
            }
            else {
                routingContext.next()
            }
        }
        router.route("/test1.css").handler { routingContext ->
            val headers = routingContext.request().headers()
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names()))
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(routingContext.request().absoluteURI())
            routingContext.response().end("#test1 { margin: 0 auto; }")
        }

        router.route("/test2.css").handler { routingContext ->
            val headers = routingContext.request().headers()
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names()))
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            cc.vileda.kalfor.mock.StaticFileServerMock.Companion.LOGGER.info(routingContext.request().absoluteURI())
            routingContext.response().end("#test2 { margin: 0 auto; }")
        }

        httpServer.requestHandler({ router.accept(it) }).listen(port)
    }

    companion object {
        private val LOGGER = io.vertx.core.logging.LoggerFactory.getLogger(cc.vileda.kalfor.mock.StaticFileServerMock::class.java)
    }
}
