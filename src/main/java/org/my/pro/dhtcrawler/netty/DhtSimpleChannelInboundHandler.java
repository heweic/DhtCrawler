package org.my.pro.dhtcrawler.netty;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.GsonUtils;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

@Sharable
public class DhtSimpleChannelInboundHandler extends MessageToMessageDecoder<KrpcMessage> {


	@Override
	protected void decode(ChannelHandlerContext ctx, KrpcMessage msg, List<Object> out) throws Exception {

		if (msg instanceof DefaultRequest) {
			out.add((DefaultRequest) msg);

		} else if (msg instanceof DefaultResponse) {

			out.add((DefaultResponse) msg);

		}

	}

}
