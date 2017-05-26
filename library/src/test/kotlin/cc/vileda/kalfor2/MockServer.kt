package cc.vileda.kalfor2

import com.github.salomonbrys.kotson.jsonObject
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.net.ServerSocket

fun mockServer(): Int {
    print("starting mock server...")
    val port = ServerSocket(0).let {
        val port = it.localPort
        it.close()
        port
    }
    embeddedServer(Netty, port) {
        routing {
            get("/") {
                call.respondText(jsonObject("foo" to "bar").toString(), ContentType.Application.Json)
            }
            get("/301") {
                call.respondRedirect("/404", true)
            }
        }
    }.start()
    Thread.sleep(400)
    println("done")

    return port
}