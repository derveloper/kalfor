package cc.vileda.kalfor;

import cc.vileda.kalfor.verticle.KalforVerticle;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;


public class Kalfor
{
	private final static Logger LOGGER = LoggerFactory.getLogger(Kalfor.class);

	public void listen(final int port)
	{
		final Vertx vertx = Vertx.vertx();
		final Observable<String> verticle = RxHelper.deployVerticle(vertx, new KalforVerticle(port));
		verticle.subscribe(LOGGER::info);
	}
}
