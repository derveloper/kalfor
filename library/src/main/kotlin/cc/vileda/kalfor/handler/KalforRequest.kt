package cc.vileda.kalfor.handler


@Suppress("unused")
class KalforRequest {
    var proxyBaseUrl: String = ""

    var headers: List<KalforProxyHeader>? = null

    var proxyRequests: List<KalforProxyRequest>? = null

    constructor() {
    }

    constructor(proxyBaseUrl: String, headers: List<KalforProxyHeader>, proxyRequests: List<KalforProxyRequest>) {
        this.proxyBaseUrl = proxyBaseUrl
        this.headers = headers
        this.proxyRequests = proxyRequests
    }
}
