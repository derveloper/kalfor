package cc.vileda.kalfor;

import cc.vileda.kalfor.core.Endpoint;
import cc.vileda.kalfor.core.KalforOptions;
import cc.vileda.kalfor.verticle.KalforVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import java.net.MalformedURLException;


public class Kalfor
{
	private final static Logger LOGGER = LoggerFactory.getLogger(Kalfor.class);
	private final Endpoint endpoint;

	public Kalfor(final String url) throws MalformedURLException
	{
		this.endpoint = new Endpoint(url);
	}

	public void listen(final int port)
	{
		final Vertx vertx = Vertx.vertx();

		final KalforOptions kalforOptions = new KalforOptions(endpoint, port);
		final Observable<String> verticle = RxHelper.deployVerticle(vertx, new KalforVerticle(kalforOptions));
		verticle.subscribe(LOGGER::info);
	}
}
