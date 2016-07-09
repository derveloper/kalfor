package cc.vileda.kalfor.handler;

import java.util.List;


public class KalforRequest
{
	@SuppressWarnings("WeakerAccess")
	public String proxyBaseUrl;

	@SuppressWarnings("WeakerAccess")
	public List<KalforProxyHeader> headers;

	@SuppressWarnings("WeakerAccess")
	public List<KalforProxyRequest> proxyRequests;

	@SuppressWarnings("unused")
	public KalforRequest()
	{
	}

	public KalforRequest(final String proxyBaseUrl, final List<KalforProxyHeader> headers, final List<KalforProxyRequest> proxyRequests)
	{
		this.proxyBaseUrl = proxyBaseUrl;
		this.headers = headers;
		this.proxyRequests = proxyRequests;
	}
}
