package cc.vileda

import cc.vileda.kalfor2.KalforRequest
import cc.vileda.kalfor2.kalfor
import cc.vileda.kalfor2.validateSchema
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.post
import org.jetbrains.ktor.routing.routing

fun main(args: Array<String>) {
    print("starting kalfor server...")
    embeddedServer(Netty, 8080) {
        routing {
            post("/combine") {
                val json = call.request.receive(String::class)
                val resp = validateSchema(json)
                        .fold({
                            Gson().toJson(kalfor(Gson()
                                    .fromJson<List<KalforRequest>>(json)))
                        }, {
                            it.printStackTrace()
                            it.message!!
                        })
                call.respondText(resp)
            }
        }
    }.start(wait = true)
}