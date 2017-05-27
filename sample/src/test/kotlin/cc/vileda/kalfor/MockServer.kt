package cc.vileda.kalfor

import cc.vileda.kalfor.service.kalforService
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

fun mockKalforService(): Int {
    print("starting mock server...")
    return ServerSocket(0).let {
        val port = it.localPort
        it.close()
        kalforService(false, port)
        Thread.sleep(1000)
        println("done")
        port
    }
}

fun mockServer2(): Int {
    print("starting mock server...")
    return ServerSocket(0).let {
        val port = it.localPort
        it.close()
        embeddedServer(Netty, port) {
            routing {
                get("/") {
                    call.respondText(jsonObject("foo" to "bar").toString(), ContentType.Application.Json)
                }
                get("/301") {
                    call.respondRedirect("/404", true)
                }
                get("/res-text") {
                    call.respondText("test-1")
                }
                get("/res-text2") {
                    call.respondText("test-2")
                }

            }
        }.start()
        Thread.sleep(1000)
        println("done")
        port
    }
}