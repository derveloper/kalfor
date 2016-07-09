package cc.vileda.kalfor.handler

@Suppress("unused")
class KalforProxyRequest {
    var key: String = ""
    var path: String = ""

    constructor(key: String, path: String) {
        this.path = path
        this.key = key
    }

    constructor()
}
