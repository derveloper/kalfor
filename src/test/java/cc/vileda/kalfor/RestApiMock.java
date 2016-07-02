package cc.vileda.kalfor;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.MultiMap;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;

import java.util.stream.Collectors;


class RestApiMock extends AbstractVerticle
{
	private final static Logger LOGGER = LoggerFactory.getLogger(RestApiMock.class);
	private final int port;

	RestApiMock(final int port)
	{
		this.port = port;
	}

	@Override
	public void start() throws Exception
	{
		final HttpServer httpServer = vertx.createHttpServer();
		final Router router = Router.router(vertx);
		router.route("/test").handler(routingContext -> {
			final MultiMap headers = routingContext.request().headers();
			LOGGER.info(Json.encodePrettily(headers.names().stream().map(headers::getAll).collect(Collectors.toList())));
			routingContext.response()
					.putHeader("content-type", "application/json")
					.end(new JsonObject().put("foo", "bar").encodePrettily());
		});

		httpServer.requestHandler(router::accept).listen(port);
	}
}
