package cc.vileda.kalfor.handler

import java.net.MalformedURLException
import java.net.URL


class Endpoint @Throws(MalformedURLException::class)
constructor(baseUrl: String) {
    private val parsed: URL

    init {
        this.parsed = URL(baseUrl)
    }

    val isSSL: Boolean?
        get() = "https" == scheme()

    fun scheme(): String {
        return parsed.protocol
    }

    fun host(): String {
        return parsed.host
    }

    fun port(): Int {
        if (parsed.port == -1) {
            return parsed.defaultPort
        } else {
            return parsed.port
        }
    }
}
