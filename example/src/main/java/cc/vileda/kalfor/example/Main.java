package cc.vileda.kalfor.example;

import cc.vileda.kalfor.Kalfor;


class Main
{
	static {
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
	}

	public static void main(String[] args)
	{
		new Kalfor().listen(8080);
	}
}
