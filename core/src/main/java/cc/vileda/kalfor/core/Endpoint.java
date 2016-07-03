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

	Boolean isSSL()
	{
		return Objects.equals("https", scheme());
	}

	private String scheme()
	{
		return parsed.getProtocol();
	}

	String host()
	{
		return parsed.getHost();
	}

	int port()
	{
		if (parsed.getPort() == -1) { return parsed.getDefaultPort(); }
		else { return parsed.getPort(); }
	}
}
