package cc.vileda.kalfor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;


class Endpoint
{
		private final URL parsed;

		Endpoint(final String baseUrl) throws MalformedURLException
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
