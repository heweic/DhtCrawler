package org.my.pro.dhtcrawler.netty;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.AbstractDhtNode;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.util.GsonUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class DefaultDhtNode extends AbstractDhtNode {

	private EventLoopGroup group;
	private Bootstrap bootstrap;
	private ChannelFuture channelFuture;

	private static Log log = LogFactory.getLog(DefaultDhtNode.class);

	private ChannelHandler channelHandler;

	private Thread nodeThread;

	public DefaultDhtNode(byte[] id, int port) {
		super(id, port);
		//

	}

	
	@Override
	public void sendMessage(KrpcMessage krpcMessage) {
		channelFuture.channel().writeAndFlush(krpcMessage);
	}


	@Override
	public void setChannelHandler(ChannelHandler channelHandler) {
		this.channelHandler = channelHandler;
	}

	@Override
	public Channel channel() {
		return channelFuture.channel();
	}

	private CountDownLatch countDownLatch = new CountDownLatch(1);

	@Override
	public void start() {

		nodeThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					group = new NioEventLoopGroup();

					bootstrap = new Bootstrap();
					bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true);
					bootstrap.handler(channelHandler);

					channelFuture = bootstrap.bind(port()).sync();
					channelFuture.addListener(future -> {
						if (future.isSuccess()) {
							log.info("启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + channelFuture.channel().localAddress());
						} else {
							log.info("启动节点:" + GsonUtils.GSON.toJson(id()) + "-" + port() + "失败!");
						}
						countDownLatch.countDown();
					});

					
					channelFuture.channel().closeFuture().await();

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("节点启动失败..." + port());
				} finally {
					group.shutdownGracefully();
				}

			}
		});

		nodeThread.start();

		//
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void stop() {
		if (null != channelFuture.channel()) {
			channelFuture.channel().close();
		}
	}

	@Override
	public boolean isRun() {
		return channelFuture != null && channelFuture.channel().isActive();
	}

}
