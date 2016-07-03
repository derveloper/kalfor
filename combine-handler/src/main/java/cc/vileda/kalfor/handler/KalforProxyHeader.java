package cc.vileda.kalfor.handler;

public class KalforProxyHeader
{
	@SuppressWarnings("WeakerAccess")
	public String name;

	@SuppressWarnings("WeakerAccess")
	public String value;

	@SuppressWarnings("unused")
	public KalforProxyHeader()
	{
	}

	public KalforProxyHeader(final String name, final String value)
	{
		this.name = name;
		this.value = value;
	}
}
