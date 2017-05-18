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
                .onErrorReturn { _ -> Observable.empty() }
                .doOnError { LOGGER.error(it.message, it) }
                .flatMap { it }
                .reduce("", aggregateResponse())
                .doOnError { LOGGER.error(it.message, it) }
                .onErrorReturn { "" }
                .defaultIfEmpty("")
                .subscribe(respondToClient(request, response))
    }
}
