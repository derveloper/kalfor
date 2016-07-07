package cc.vileda.kalfor.handler;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.io.IOException;
import java.net.HttpURLConnection;


public class SchemaValidationHandler implements Handler<RoutingContext>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidationHandler.class);
	private final JsonSchema schema;

	public SchemaValidationHandler()
	{
		try {
			schema = JsonSchemaFactory.byDefault().getJsonSchema("resource:/kalfor-schema.json");
		}
		catch (ProcessingException e) {
			LOGGER.error(e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handle(final RoutingContext routingContext)
	{
		try {
			final ProcessingReport validate = schema.validate(JsonLoader.fromString(routingContext.getBodyAsString()));
			if(validate.isSuccess()) {
				routingContext.next();
				return;
			}

			final JsonArray messages = new JsonArray();
			validate.forEach(processingMessage -> messages.add(processingMessage.getMessage()));
			final JsonObject error = new JsonObject().put("error", messages);
			routingContext.response()
					.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
					.end(error.encodePrettily());
		}
		catch (ProcessingException | IOException e) {
			LOGGER.error(e);
			final JsonObject error = new JsonObject().put("error", e.getMessage());
			routingContext.response()
					.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST)
					.end(error.encodePrettily());
		}
	}
}
