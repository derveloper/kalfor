package cc.vileda.kalfor.handler;

import java.util.List;


public class KalforRequest
{
	@SuppressWarnings("WeakerAccess")
	public String proxyBaseUrl;

	@SuppressWarnings("WeakerAccess")
	public List<KalforProxyRequest> proxyRequests;

	@SuppressWarnings("unused")
	public KalforRequest()
	{
	}

	public KalforRequest(final String proxyBaseUrl, final List<KalforProxyRequest> proxyRequests)
	{
		this.proxyBaseUrl = proxyBaseUrl;
		this.proxyRequests = proxyRequests;
	}
}
