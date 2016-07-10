package cc.vileda.kalfor.handler

@Suppress("unused")
class KalforProxyHeader {
    var name: String = ""

    var value: String = ""

    constructor() {
    }

    constructor(name: String, value: String) {
        this.name = name
        this.value = value
    }
}
