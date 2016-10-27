package cc.vileda.kalfor.handler


data class KalforRequest(
        var proxyBaseUrl: String = "",
        var headers: List<KalforProxyHeader> = mutableListOf(),
        var proxyRequests: List<KalforProxyRequest> = mutableListOf()
)
