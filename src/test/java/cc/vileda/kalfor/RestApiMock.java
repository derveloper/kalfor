package cc.vileda.kalfor;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;


class RestApiMock extends AbstractVerticle
{
		private final int port;

		public RestApiMock(final int port)
		{
				this.port = port;
		}

		@Override
		public void start() throws Exception
		{
				final HttpServer httpServer = vertx.createHttpServer();
				final Router router = Router.router(vertx);
				router.route("/test").handler(routingContext ->
						routingContext.response()
								.putHeader("content-type", "application/json")
								.end(new JsonObject().put("foo", "bar").encodePrettily()));

				httpServer.requestHandler(router::accept).listen(port);
		}
}
