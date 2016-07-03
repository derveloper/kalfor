package cc.vileda.kalfor.verticle;

import cc.vileda.kalfor.core.KalforOptions;
import cc.vileda.kalfor.handler.CombineHandler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.CorsHandler;


public class KalforVerticle extends AbstractVerticle
{
	private final static Logger LOGGER = LoggerFactory.getLogger(KalforVerticle.class);

	private final KalforOptions kalforOptions;
	private HttpServer httpServer;
	private HttpClient httpClient;

	public KalforVerticle(final KalforOptions kalforOptions)
	{
		this.kalforOptions = kalforOptions;
	}

	@Override
	public void start() throws Exception
	{
		httpServer = vertx.createHttpServer();
		httpClient = createHttpClient();

		final Router router = Router.router(vertx);
		router.route().handler(CorsHandler.create("*").allowedHeader("authorization"));

		router.post("/combine").handler(new CombineHandler(httpClient, kalforOptions));
		router.get("/combine").handler(new CombineHandler(httpClient, kalforOptions));

		httpServer.requestHandler(router::accept).listen(kalforOptions.listenPort);
	}

	private HttpClient createHttpClient()
	{
		final HttpClientOptions httpClientOptions = new HttpClientOptions()
				.setDefaultHost(kalforOptions.proxyHost)
				.setSsl(kalforOptions.ssl)
				.setTrustAll(true)
				.setVerifyHost(false)
				.setDefaultPort(kalforOptions.proxyPort);

		LOGGER.info(kalforOptions.toString());

		return vertx.createHttpClient(httpClientOptions);
	}

	@Override
	public void stop() throws Exception
	{
		httpClient.close();
		httpServer.close();
	}
}
