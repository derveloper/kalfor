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
		new Kalfor("https://api.github.com").listen(8081);
	}
}
