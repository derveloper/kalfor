package cc.vileda.kalfor.handler

@Suppress("unused")
class KalforProxyRequest {
    var path: String = ""

    var key: String = ""

    constructor(path: String, key: String) {
        this.path = path
        this.key = key
    }

    constructor()
}
