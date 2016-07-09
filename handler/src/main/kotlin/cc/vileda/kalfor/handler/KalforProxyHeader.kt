package cc.vileda.kalfor.handler

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
