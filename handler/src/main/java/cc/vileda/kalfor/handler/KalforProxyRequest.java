package cc.vileda.kalfor.handler;

public class KalforProxyRequest
{
	@SuppressWarnings("WeakerAccess")
	public String path;

	@SuppressWarnings("WeakerAccess")
	public String key;

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
