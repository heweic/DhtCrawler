package org.my.pro.dhtcrawler.netty;

import org.my.pro.dhtcrawler.KrpcMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToByteEncoder;

public class BenEncoder extends MessageToByteEncoder<KrpcMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, KrpcMessage msg, ByteBuf out) throws Exception {

		try {
			byte[] bs = null;
			try {
				bs = msg.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (null != bs) {
				ctx.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(bs), msg.addr()));
			}
			// ByteBuffer
			// buffer = DHTHelper.getFindNodeData("bbbb", "abcdefghij0123456789",
			// "abcdefghij0123456723");
			// ctx.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(buffer),
			// msg.addr()));
		}catch (Exception e) {
			// TODO: handle exception
		}
	}

}
