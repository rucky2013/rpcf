package com.rpcf.server;

import java.net.InetSocketAddress;
import java.security.Policy.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.transform.Result;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.rpcf.common.Entry;
import com.rpcf.common.Request;
import com.rpcf.common.Response;
import com.rpcf.method.MethodSupport;
import com.rpcf.network.handler.Decoder;
import com.rpcf.network.handler.Encoder;
import com.rpcf.network.handler.ParametersHandler;
import com.rpcf.serialize.Serializer;

public class Server {
	private final int port;

	private final List<BlockingDeque<Entry<Channel, byte[]>>> deques;

	private volatile boolean isStart;

	private MethodSupport methodSupport;

	private final int threadCount;

	private ChannelFactory factory;

	private ServerBootstrap serverBootstrap;

	private ExecutorService executor;

	private AtomicLong pos;

	public Server(int port, int threadCount) {
		this.port = port;
		this.threadCount = threadCount;
		isStart = false;
		methodSupport = new MethodSupport();
		deques = new ArrayList<BlockingDeque<Entry<Channel, byte[]>>>(8);
		for (int index = 0; index < 8; ++index) {
			deques.add(new LinkedBlockingDeque<Entry<Channel, byte[]>>());
		}
		pos = new AtomicLong(0);
	}

	public synchronized void start() {
		if (!isStart) {
			isStart = true;

			factory = new NioServerSocketChannelFactory(Executors.newFixedThreadPool(2), Executors.newFixedThreadPool(8), 8);
			serverBootstrap = new ServerBootstrap(factory);
			serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

				public ChannelPipeline getPipeline() throws Exception {
					return Channels.pipeline(new Decoder(), new ParametersHandler(Server.this), new Encoder());
				}
			});
			serverBootstrap.setOption("child.keepAlive", true);
			serverBootstrap.bind(new InetSocketAddress(port));

			executor = Executors.newFixedThreadPool(threadCount);
			for (int count = 0; count < threadCount; ++count) {
				final int seed = count % 8;
				executor.execute(new Runnable() {
					public void run() {
						Server.this.execute(seed);
					}
				});
			}
		} else {
			throw new ServerStartException();
		}
	}

	public synchronized void stop() {
		if (isStart) {
			isStart = false;
		}
	}

	public void execute(int seed) {
		while (isStart) {
			Class<?>[] clazzArray = new Class<?>[1];

			Entry<Channel, byte[]> entry = null;
			try {
				entry = deques.get(seed++ % 8).takeFirst();
				System.out.println(Thread.currentThread().getName());
			} catch (InterruptedException e) {
				continue;
			}
			Request request = null;
			try {
				request = Serializer.deserializer(entry.getColumn(), Request.class);

				Object result = methodSupport.invoke(request.getInterfaceName(), request.getInvokeMethod(), request.getParameterTypes().toArray(clazzArray), request.getParameters().toArray());

				entry.getRow().write(Serializer.serialize(Response.success(result, request.getId())));
			} catch (Exception e) {
				e.printStackTrace();
				entry.getRow().write(this.getFailure("方法调用失败(没有此方法)" + e.getMessage(), request.getId()));
			}
		}
	}

	public byte[] getFailure(String msg, Long reqId) {
		byte[] failbytes = Serializer.serialize(Response.fail(msg, reqId));
		return failbytes;
	}

	public void putParameters(Channel channel, byte[] parameters) {
		if (isStart) {
			deques.get((int) (pos.getAndIncrement() % 8)).offerLast(Entry.<Channel, byte[]> getEntry(channel, parameters));
		} else {
			throw new ServerStopException();
		}
	}

	public void register(String className, Object impl) {
		methodSupport.register(className, impl);
	}
}
