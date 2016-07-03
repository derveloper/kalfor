package cc.vileda.kalfor.handler;

public class KalforProxyRequest
{
	String key;
	String path;

	@SuppressWarnings("unused")
	public KalforProxyRequest()
	{
	}

	public KalforProxyRequest(final String key, final String path)
	{
		this.key = key;
		this.path = path;
	}
}
