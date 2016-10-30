package cc.vileda.kalfor.handler

import io.vertx.core.Handler
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.ext.web.RoutingContext
import rx.Observable


class CombineHandler(private val vertx: Vertx) : Handler<RoutingContext> {
    override fun handle(routingContext: RoutingContext) {
        val request = routingContext.request()
        val response = routingContext.response()

        Observable.from(parseRequest(routingContext, vertx))
                .doOnEach(::println)
                .doOnError { it.printStackTrace() }
                .map(::convertResponseStrategy)
                .onErrorReturn { throwable -> Observable.empty() }
                .doOnError { it.printStackTrace() }
                .flatMap { it }
                .reduce("", aggregateResponse())
                .doOnError { it.printStackTrace() }
                .onErrorReturn { "" }
                .defaultIfEmpty("")
                .subscribe(respondToClient(request, response))
    }
}
