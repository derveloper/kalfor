package cc.vileda.kalfor.handler;

import io.vertx.rxjava.core.buffer.Buffer;


class Context
{
	final String name;
	final Buffer buffer;

	Context(final String name, final Buffer buffer)
	{
		this.name = name;
		this.buffer = buffer;
	}
}
