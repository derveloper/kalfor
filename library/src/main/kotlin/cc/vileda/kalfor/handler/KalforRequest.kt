package cc.vileda.kalfor.handler

import io.vertx.core.http.HttpMethod


data class KalforRequest(
        var proxyBaseUrl: String = "",
        var headers: List<KalforProxyHeader> = mutableListOf(),
        var proxyRequests: List<KalforProxyRequest> = mutableListOf(),
        val type: HttpMethod = HttpMethod.POST
)
