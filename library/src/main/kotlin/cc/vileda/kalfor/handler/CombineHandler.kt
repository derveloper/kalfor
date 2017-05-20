package cc.vileda.kalfor.handler

import io.vertx.core.Handler
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.ext.web.RoutingContext
import rx.Observable


class CombineHandler(private val vertx: Vertx) : Handler<RoutingContext> {
    override fun handle(routingContext: RoutingContext) {
        val request = routingContext.request()
        val response = routingContext.response()

        parseRequest(routingContext, vertx)
                .map(::convertResponse)
                .flatMap { it }
                .reduce("", aggregateResponse())
                .defaultIfEmpty("")
                .subscribe(respondToClient(request, response))
    }
}
