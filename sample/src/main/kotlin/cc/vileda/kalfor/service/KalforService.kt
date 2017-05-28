package cc.vileda.kalfor.service

import cc.vileda.kalfor.*
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import org.jetbrains.ktor.routing.routing
import java.net.URL

fun main(args: Array<String>) {
    print("starting kalforPost server...")
    kalforService(true, 8080)
}

fun kalforService(wait: Boolean, port: Int) {
    embeddedServer(Netty, port) {
        routing {
            post("/combine") {
                val json = call.request.receive(String::class)
                val resp = validateSchema(json)
                        .fold({
                            Gson().toJson(kalforPost(Gson()
                                    .fromJson<List<KalforRequest>>(json)))
                        }, {
                            it.message!!
                        })
                call.respondText(resp)
            }
            get("/combine") {
                call.respondText(kalforGet((call.request.queryParameters["c"] ?: "").split(",").map {
                    val uri = URL(it)
                    KalforRequest("${uri.protocol}://${uri.host}:${uri.port}",
                            emptyList(),
                            listOf(KalforProxyRequest(it, uri.path)))
                }))
            }
        }
    }.start(wait)
}