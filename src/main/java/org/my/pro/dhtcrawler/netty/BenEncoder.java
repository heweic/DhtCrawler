package org.my.pro.dhtcrawler.netty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.util.GsonUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToByteEncoder;

public class BenEncoder extends MessageToByteEncoder<KrpcMessage> {

	private static Log log = LogFactory.getLog(BenEncoder.class);

	@Override
	protected void encode(ChannelHandlerContext ctx, KrpcMessage msg, ByteBuf out) throws Exception {
		byte[] bs = null;
		try {
			bs = msg.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		if (null != bs) {
			// log.info("发送消息:" + msg.addr() + GsonUtils.toJsonString(msg));
			ctx.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(bs), msg.addr()));
		}
	}

}
