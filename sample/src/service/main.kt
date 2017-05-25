package cc.vileda.service

import cc.vileda.kalfor.handler.LOGGER
import io.vertx.rxjava.core.RxHelper
import io.vertx.rxjava.core.Vertx

object KalforMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val vertx = Vertx.vertx()
        val listenPort = if (args.size == 1) args[0].toInt() else 8080
        val deployVerticle = RxHelper.deployVerticle(vertx, KalforVerticle(listenPort))
        deployVerticle.subscribe {
            LOGGER.info(it)
        }
    }
}
