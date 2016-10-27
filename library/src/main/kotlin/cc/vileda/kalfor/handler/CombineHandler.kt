package cc.vileda.kalfor.handler

import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.ext.web.RoutingContext
import java.util.concurrent.TimeUnit


class CombineHandler(private val vertx: Vertx) : Handler<RoutingContext> {
    override fun handle(routingContext: RoutingContext) {
        val request = routingContext.request()
        val response = routingContext.response()

        parseRequest(routingContext)
                .flatMap({ proxyRequest(it, request, vertx) })
                .timeout(5, TimeUnit.SECONDS)
                .reduce(JsonObject(), aggregateResponse())
                .doOnError { it.printStackTrace() }
                .onErrorReturn { throwable -> JsonObject() }
                .subscribe(respondToClient(request, response))
    }
}
