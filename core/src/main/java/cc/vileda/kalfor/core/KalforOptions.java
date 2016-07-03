package cc.vileda.kalfor.core;

public class KalforOptions
{
	public final boolean ssl;
	public final String proxyHost;
	public final int proxyPort;
	public final int listenPort;

	public KalforOptions(final Endpoint endpoint, final int listenPort)
	{
		this.ssl = endpoint.isSSL();
		this.proxyHost = endpoint.host();
		this.proxyPort = endpoint.port();
		this.listenPort = listenPort;
	}

	@Override
	public String toString()
	{
		return "KalforOptions{" +
				"ssl=" + ssl +
				", proxyHost='" + proxyHost + '\'' +
				", proxyPort=" + proxyPort +
				", listenPort=" + listenPort +
				'}';
	}
}
