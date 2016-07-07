package cc.vileda.kalfor.handler;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;


@RunWith(VertxUnitRunner.class)
public class CombineHandlerTest
{
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
				new KalforTestVerticle(kalforPort)
		);
		deployVerticle
				.subscribe(s -> {
					System.out.println("deployed Api Mock "+s);
					deployVerticle2.subscribe(s1 -> {
						System.out.println("deployed Kalfor " + s1);
						async.complete();
					});
				});
	}

	@Test
	public void restApiMockShouldRespond()
	{
		RestAssured.get("http://localhost:" + remotePort + "/test")
				.then()
				.body(Matchers.containsString(new JsonObject().put("foo", "bar").encodePrettily()));
	}

	@Test
	public void combineHandlerShouldCombine()
	{
		final String given = new JsonArray(Collections.singletonList(
				new KalforRequest(
						"http://localhost:" + remotePort,
						Collections.singletonList(new KalforProxyHeader("x-foo", "bar")),
						Arrays.asList(
								new KalforProxyRequest("firstKey", "/test"),
								new KalforProxyRequest("secondKey", "/test")
						)
				)
		)).encodePrettily();

		final String expected = new JsonObject()
				.put("firstKey", new JsonObject().put("foo", "bar"))
				.put("secondKey", new JsonObject().put("foo", "bar"))
				.encodePrettily();
		RestAssured.given()
				.body(given)
				.header(new Header("Content-Type", "application/json"))
				.when()
				.post("http://localhost:" + kalforPort + "/combine")
				.then()
				.body(Matchers.containsString(expected));
	}

	@Test
	public void combineHandlerShouldRespondWithBadRequestOnInvalidRequest()
	{
		final String given = new JsonArray(Arrays.asList(
				new JsonObject().put("foo", "bar"),
				new JsonObject().put("baz", new JsonObject().put("bar", "foo"))
		)).encodePrettily();

		RestAssured.given()
				.body(given)
				.header(new Header("Content-Type", "application/json"))
				.when()
				.post("http://localhost:" + kalforPort + "/combine")
				.then()
				.statusCode(Matchers.is(HttpStatus.SC_BAD_REQUEST))
				.body(Matchers.containsString("{\n" +
						"  \"error\" : [ \"object has missing required properties ([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\", \"object has missing required " +
						"properties ([\\\"proxyBaseUrl\\\",\\\"proxyRequests\\\"])\" ]\n" +
						"}"));
	}
}
