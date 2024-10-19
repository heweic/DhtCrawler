package org.my.pro.dhtcrawler.util;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class UTPTest {

	public static void main(String[] args) {
		new UTPTest().start();
		
		 long time = System.currentTimeMillis();
		 System.out.println(time);
	}

	public void start() {
		EventLoopGroup group;
		Bootstrap bootstrap;
		ChannelFuture channelFuture;
		group = new NioEventLoopGroup();

		bootstrap = new Bootstrap();
		bootstrap.group(group).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true)
				.option(ChannelOption.SO_RCVBUF, 1048576).option(ChannelOption.SO_SNDBUF, 1048576)
				.option(ChannelOption.SO_REUSEADDR, true)

		;
		bootstrap.handler(new Handler());

		try {
			channelFuture = bootstrap.bind(0).sync();

			channelFuture.channel().closeFuture().await();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class Handler extends SimpleChannelInboundHandler<DatagramPacket> {

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ByteBuf buf = Unpooled.buffer();

			buf.writeByte((byte) 0);// ack
			buf.writeByte((byte) 1);// SYN
			buf.writeByte((byte) 0);// FIN
			buf.writeByte((byte) 0);// DATA
//			buf.writeInt(1);
			
			buf.writeInt(1); // Version
			buf.writeBytes(new byte[8]);// 8 bits 扩展字段
			buf.writeBytes(new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4}); // 连接ID {0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,4}
			
			//buf.writeBytes(new byte[32]); // 发送时间戳

			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(System.currentTimeMillis());
			
			
//			buf.writeBytes(new byte[32]); // 上个包的时间差
			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(0);


			
//			buf.writeBytes(new byte[32]); // 当前可用接收窗口大小			
			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(0);
			buf.writeLong(1048576);

		//	buf.writeBytes(new byte[16]); // 当前数据包的序列号
			buf.writeLong(0);
			buf.writeLong(0);
	
			
		//	buf.writeBytes(new byte[16]); // 期望下一包的序列号
			buf.writeLong(0);
			buf.writeLong(1);
		

			ctx.writeAndFlush(new DatagramPacket(buf, new InetSocketAddress("195.154.181.225", 55024)));
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {

		}

	}
}
