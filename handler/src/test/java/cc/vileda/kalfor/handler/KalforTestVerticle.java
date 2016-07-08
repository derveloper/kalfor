package cc.vileda.kalfor.handler;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;


class KalforTestVerticle extends AbstractVerticle
{
	private final int listenPort;

	KalforTestVerticle(final int listenPort)
	{
		this.listenPort = listenPort;
	}

	@Override
	public void start() throws Exception
	{
		final HttpServer httpServer = vertx.createHttpServer();

		final Router router = Router.router(vertx);
		router.route().handler(CorsHandler.create("*").allowedHeader("authorization"));
		router.route().handler(BodyHandler.create());

		router.route().handler(new SchemaValidationHandler());
		router.route().handler(new CombineHandler(vertx));

		httpServer.requestHandler(router::accept).listen(listenPort);
	}
}
