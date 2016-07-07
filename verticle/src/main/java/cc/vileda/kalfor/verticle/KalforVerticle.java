package cc.vileda.kalfor.verticle;

import cc.vileda.kalfor.handler.CombineHandler;
import cc.vileda.kalfor.handler.SchemaValidationHandler;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;


public class KalforVerticle extends AbstractVerticle
{
	private final int listenPort;
	private HttpServer httpServer;

	@SuppressWarnings("unused")
	public KalforVerticle()
	{
		this(8080);
	}

	public KalforVerticle(final int listenPort)
	{
		this.listenPort = listenPort;
	}

	@Override
	public void start() throws Exception
	{
		httpServer = vertx.createHttpServer();

		final Router router = Router.router(vertx);
		router.route().handler(CorsHandler.create("*").allowedHeader("authorization"));
		router.route().handler(BodyHandler.create());

		router.post("/combine").handler(new SchemaValidationHandler());
		router.post("/combine").handler(new CombineHandler(vertx));

		httpServer.requestHandler(router::accept).listen(listenPort);
	}

	@Override
	public void stop() throws Exception
	{
		httpServer.close();
	}
}
