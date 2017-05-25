package cc.vileda.kalfor2

import io.vertx.core.http.HttpMethod


data class KalforRequest(
        val proxyBaseUrl: String = "",
        val headers: List<KalforProxyHeader> = emptyList(),
        var proxyRequests: List<KalforProxyRequest> = emptyList(),
        val type: HttpMethod = HttpMethod.POST
)
