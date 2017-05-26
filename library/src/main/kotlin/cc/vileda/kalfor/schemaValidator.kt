package cc.vileda.kalfor

import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import org.funktionale.tries.Try

private val schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/kalfor-schema.json")

fun validateSchema(json: String): Try<Boolean> {
    val validationResult = schema.validate(JsonLoader.fromString(json))
    return if (validationResult.isSuccess) {
        Try.Success(validationResult.isSuccess)
    } else {
        val messages = validationResult.fold(emptyList<String>()) { list, processingMessage ->
            list + processingMessage.message
        }.joinToString("\n")
        Try.Failure(RuntimeException(messages))
    }
}
