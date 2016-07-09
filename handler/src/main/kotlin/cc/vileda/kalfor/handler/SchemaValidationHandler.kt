package cc.vileda.kalfor.handler

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.ext.web.RoutingContext

import java.io.IOException
import java.net.HttpURLConnection


class SchemaValidationHandler : Handler<RoutingContext> {
    private val schema: JsonSchema

    init {
        try {
            schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/kalfor-schema.json")
        } catch (e: ProcessingException) {
            LOGGER.error(e)
            throw RuntimeException(e)
        }

    }

    override fun handle(routingContext: RoutingContext) {
        try {
            val validate = schema.validate(JsonLoader.fromString(routingContext.bodyAsString))
            if (validate.isSuccess) {
                routingContext.next()
                return
            }

            val messages = JsonArray()
            validate.forEach { processingMessage -> messages.add(processingMessage.message) }
            val error = JsonObject().put("error", messages)
            routingContext.response().setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST).end(error.encodePrettily())
        } catch (e: ProcessingException) {
            LOGGER.error(e)
            val error = JsonObject().put("error", e.message)
            routingContext.response().setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST).end(error.encodePrettily())
        } catch (e: IOException) {
            LOGGER.error(e)
            val error = JsonObject().put("error", e.message)
            routingContext.response().setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST).end(error.encodePrettily())
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SchemaValidationHandler::class.java)
    }
}
