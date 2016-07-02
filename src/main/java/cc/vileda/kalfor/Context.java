package cc.vileda.kalfor;

import io.vertx.rxjava.core.buffer.Buffer;


class Context
{
	public final String name;
	public final String path;
	public final Buffer buffer;

	public Context(final String name, final String path, final Buffer buffer)
	{
		this.name = name;
		this.path = path;
		this.buffer = buffer;
	}
}
