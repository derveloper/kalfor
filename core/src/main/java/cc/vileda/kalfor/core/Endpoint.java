package cc.vileda.kalfor.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;


public class Endpoint
{
	private final URL parsed;

	public Endpoint(final String baseUrl) throws MalformedURLException
	{
		this.parsed = new URL(baseUrl);
	}

	public Boolean isSSL()
	{
		return Objects.equals("https", scheme());
	}

	public String scheme()
	{
		return parsed.getProtocol();
	}

	public String host()
	{
		return parsed.getHost();
	}

	public int port()
	{
		if (parsed.getPort() == -1) { return parsed.getDefaultPort(); }
		else { return parsed.getPort(); }
	}
}
