package cc.vileda.kalfor;

import cc.vileda.kalfor.core.Endpoint;
import cc.vileda.kalfor.core.KalforOptions;
import cc.vileda.kalfor.verticle.KalforVerticle;
import io.restassured.http.Header;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;


@RunWith(VertxUnitRunner.class)
public class CombineHandlerTest
{
	static {
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
	}

	private final int kalforPort = getRandomPort();
	private final int remotePort = getRandomPort();

	public CombineHandlerTest() throws IOException {}

	private static int getRandomPort() throws IOException
	{
		ServerSocket socket = new ServerSocket(0);
		int port = socket.getLocalPort();
		socket.close();

		return port;
	}

	@Before
	public void setUp(TestContext context) throws IOException
	{
		final Vertx vertx = Vertx.vertx();
		final Async async = context.async();
		final Observable<String> deployVerticle = RxHelper.deployVerticle(vertx, new RestApiMock(remotePort));
		final Observable<String> deployVerticle2 = RxHelper.deployVerticle(
				vertx,
				new KalforVerticle(new KalforOptions(new Endpoint("http://api.local:" + remotePort), kalforPort))
		);
		deployVerticle
				.subscribe(s -> {
					System.out.println(s);
					deployVerticle2.subscribe(s1 -> {
						System.out.println(s1);
						async.complete();
					});
				});
	}

	@Test
	public void restApiMockShouldRespond()
	{
		get("http://combine.local:" + remotePort + "/test")
				.then()
				.body(containsString(new JsonObject().put("foo", "bar").encodePrettily()));
	}

	@Test
	public void combineHandlerShouldCombine()
	{
		final String given = new JsonArray(Arrays.asList(
				new JsonObject().put("firstKey", "/test"),
				new JsonObject().put("secondKey", "/test")
		)).encodePrettily();
		final String expected = new JsonObject()
				.put("firstKey", new JsonObject().put("foo", "bar"))
				.put("secondKey", new JsonObject().put("foo", "bar"))
				.encodePrettily();
		given()
				.body(given)
				//.header(new Header("Host", "some.api.local"))
				.header(new Header("Content-Type", "application/json"))
				.when()
				.post("http://combine.local:" + kalforPort + "/combine")
				.then()
				.body(containsString(expected));
	}
}
