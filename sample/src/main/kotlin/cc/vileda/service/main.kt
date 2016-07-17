package cc.vileda.service

import io.vertx.rxjava.core.RxHelper
import io.vertx.rxjava.core.Vertx

object KalforMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val vertx = Vertx.vertx()
        val deployVerticle = RxHelper.deployVerticle(vertx, KalforVerticle(8080))
        deployVerticle.subscribe {
            println(it)
        }
    }
}
