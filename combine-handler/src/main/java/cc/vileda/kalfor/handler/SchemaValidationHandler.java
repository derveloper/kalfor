package cc.vileda.kalfor.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.ext.web.RoutingContext;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;


public class SchemaValidationHandler implements Handler<RoutingContext>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaValidationHandler.class);
	private final Schema schema;

	public SchemaValidationHandler()
	{
		try (InputStream inputStream = getClass().getResourceAsStream("/kalfor-schema.json")) {
			final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
			schema = SchemaLoader.load(rawSchema);
		}
		catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handle(final RoutingContext routingContext)
	{
		try {
			schema.validate(new JSONArray(routingContext.getBodyAsString()));
			routingContext.next();
		} catch (final ValidationException validationException) {
			final JsonObject error = new JsonObject().put("error", validationException.getMessage());
			final JsonArray errorMessages = new JsonArray();

			validationException.getCausingExceptions()
					.forEach(e -> errorMessages.add(e.getPointerToViolation()));

			error.put("problems", errorMessages);
			routingContext.response()
					.setStatusCode(405)
					.end(error.encodePrettily());
		}
	}
}
