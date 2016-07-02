package cc.vileda.kalfor;

import java.net.MalformedURLException;


public class Main
{
		static {
				System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
		}

		public static void main(String[] args) throws MalformedURLException
		{
				new Kalfor("https://api.sipgate.com").listen(8080);
		}
}
