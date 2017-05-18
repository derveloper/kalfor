package cc.vileda.kalfor.mock


internal class RestApi2Mock(private val port: Int) : io.vertx.rxjava.core.AbstractVerticle() {
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
        router.route("/test3").handler { routingContext ->
            val headers = routingContext.request().headers()
            cc.vileda.kalfor.mock.RestApi2Mock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names()))
            cc.vileda.kalfor.mock.RestApi2Mock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            routingContext.response().putHeader("content-type", "application/json").end(io.vertx.core.json.JsonObject().put("1foo", "1bar").encodePrettily())
        }

        router.route("/test4").handler { routingContext ->
            val headers = routingContext.request().headers()
            cc.vileda.kalfor.mock.RestApi2Mock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names()))
            cc.vileda.kalfor.mock.RestApi2Mock.Companion.LOGGER.info(io.vertx.core.json.Json.encodePrettily(headers.names().map({ headers.getAll(it) })))
            routingContext.response().putHeader("content-type", "application/json").end(io.vertx.core.json.JsonObject().put("2foo", "2bar").put("a3foo", "a3bar").encodePrettily())
        }

        httpServer.requestHandler({ router.accept(it) }).listen(port)
    }

    companion object {
        private val LOGGER = io.vertx.core.logging.LoggerFactory.getLogger(cc.vileda.kalfor.handler.RestApiMock::class.java)
    }
}
