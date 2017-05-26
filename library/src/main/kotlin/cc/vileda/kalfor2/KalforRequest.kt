package cc.vileda.kalfor2

import org.jetbrains.ktor.http.HttpMethod


data class KalforRequest(
        val proxyBaseUrl: String = "",
        val headers: List<KalforProxyHeader> = emptyList(),
        var proxyRequests: List<KalforProxyRequest> = emptyList(),
        val type: HttpMethod = HttpMethod.Post
)
