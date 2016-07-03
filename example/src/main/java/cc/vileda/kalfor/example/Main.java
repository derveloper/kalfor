package cc.vileda.kalfor.example;

import cc.vileda.kalfor.Kalfor;

import java.net.MalformedURLException;


class Main
{
	static {
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
	}

	public static void main(String[] args) throws MalformedURLException
	{
		// new Kalfor("https://api.sipgate.com").listen(8080);
		new Kalfor("http://localhost:8080").listen(8081);
	}
}
