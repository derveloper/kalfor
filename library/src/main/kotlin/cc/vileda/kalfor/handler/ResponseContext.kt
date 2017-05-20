package cc.vileda.kalfor.handler

import io.vertx.core.http.HttpMethod
import io.vertx.rxjava.core.buffer.Buffer
import rx.Observable

data class ResponseContext(
        val method: HttpMethod,
        val key: String,
        val contentType: Observable<String>,
        val bufferObservable: Observable<Buffer>,
        var body: String = "")
