package cc.vileda.kalfor;

import io.restassured.RestAssured;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import org.junit.After;
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
		private final int kalforPort = getRandomPort();
		private final int remotePort = getRandomPort();

		public CombineHandlerTest() throws IOException {}

		@Before
		public void setUp(TestContext context) throws IOException
		{
				final Vertx vertx = Vertx.vertx();
				final Async async = context.async();
				final Observable<String> deployVerticle = RxHelper.deployVerticle(vertx, new RestApiMock(remotePort));
				final Observable<String> deployVerticle2 = RxHelper.deployVerticle(
						vertx,
						new KalforVerticle(new KalforOptions(new Endpoint("http://localhost:" + remotePort), kalforPort))
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
				get("http://localhost:"+remotePort+"/test")
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
						.when()
							.post("http://localhost:"+kalforPort+"/combine")
						.then()
							.body(containsString(expected));
		}

		private static int getRandomPort() throws IOException
		{
				ServerSocket socket = new ServerSocket(0);
				int port = socket.getLocalPort();
				socket.close();

				return port;
		}
}
