package cc.vileda.kalfor.handler;

import java.util.List;


public class KalforRequest
{
	public String proxyBaseUrl;
	public List<KalforProxyRequest> proxyRequests;

	public KalforRequest()
	{
	}

	public KalforRequest(final String proxyBaseUrl, final List<KalforProxyRequest> proxyRequests)
	{
		this.proxyBaseUrl = proxyBaseUrl;
		this.proxyRequests = proxyRequests;
	}
}
