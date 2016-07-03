package cc.vileda.kalfor.handler;

public class KalforProxyRequest
{
	public String key;
	public String path;

	public KalforProxyRequest()
	{
	}

	public KalforProxyRequest(final String key, final String path)
	{
		this.key = key;
		this.path = path;
	}
}
