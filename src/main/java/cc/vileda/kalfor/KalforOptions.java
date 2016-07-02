package cc.vileda.kalfor;

public class KalforOptions
{
		public final boolean ssl;
		public final String proxyHost;
		public final int proxyPort;
		public final int listenPort;

		public KalforOptions(boolean ssl, String proxyHost, int proxyPort, int listenPort)
		{
				this.ssl = ssl;
				this.proxyHost = proxyHost;
				this.proxyPort = proxyPort;
				this.listenPort = listenPort;
		}

		public KalforOptions getInstance()
		{
				return this;
		}
}
