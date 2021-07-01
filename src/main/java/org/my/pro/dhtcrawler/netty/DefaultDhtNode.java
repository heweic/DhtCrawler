package org.my.pro.dhtcrawler.netty;

import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.ServerStarted;
import org.my.pro.dhtcrawler.domain.AbstractDhtNode;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class DefaultDhtNode extends AbstractDhtNode {

	private EventLoopGroup group;
	private Bootstrap b;
	private Channel c;
	private ServerStarted serverStarted;

	public DefaultDhtNode(String id, int port) {
		super(id, port);

	}

	@Override
	public void start(ChannelHandler channelHandler) {
		try {

			group = new NioEventLoopGroup();

			b = new Bootstrap();
			b.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true);

			b.handler(new ChannelInitializer<NioDatagramChannel>() {
				@Override
				protected void initChannel(NioDatagramChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					p.addLast(new DataServerCodec());
					p.addLast(channelHandler);
				}
			});
			ChannelFuture channelFuture = b.bind(port()).sync();

			if (channelFuture.isSuccess()) {

				if (null != serverStarted) {
					serverStarted.work(channelFuture.channel());
				}

			}
			c = channelFuture.channel();

			c.closeFuture().await();
		} catch (Exception e) {
			System.out.println("节点启动失败..." + port());
		}
	}

	@Override
	public void stop() {
		if (null != c) {
			c.close();
		}
	}

	@Override
	public boolean isRun() {
		return c != null && c.isActive();
	}

	@Override
	public void exec(KrpcMessage krpcMessage) {
		if (null == krpcMessage) {
			return;
		}
		c.writeAndFlush(krpcMessage);
	}

	public void setServerStarted(ServerStarted serverStarted) {
		this.serverStarted = serverStarted;
	}

}
