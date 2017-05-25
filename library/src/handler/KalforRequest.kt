package cc.vileda.kalfor.handler

import io.vertx.core.http.HttpMethod


data class KalforRequest(
        val proxyBaseUrl: String = "",
        val headers: List<Kalfor2ProxyHeader>? = mutableListOf(),
        var proxyRequests: List<KalforProxyRequest> = mutableListOf(),
        val type: HttpMethod = HttpMethod.POST
)
