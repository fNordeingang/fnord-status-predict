package cc.vileda.ml.fnordpredict

import io.vertx.core.Vertx

fun response(): String {
    return """obey! fnordeingang opens at ${fnordOpensInHoursFromNow(data)}!

Accuracy:
Date: 72%
Hour±1.5: 25%"""
}

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()
    val server = vertx.createHttpServer()

    server.requestHandler({ request ->
        request.response()
                .putHeader("content-type", "text/plain")
                .end(response())
    })

    server.listen(8080)
}
