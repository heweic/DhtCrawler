package org.my.pro.dhtcrawler.netty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.DefaultResponse;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class DHTResponseChannelInboundHandler extends SimpleChannelInboundHandler<DefaultResponse> {

	public static Log LOG = LogFactory.getLog(DHTResponseChannelInboundHandler.class);

	private ResponseMessageHandler responseMessageHandler;

	public DHTResponseChannelInboundHandler(ResponseMessageHandler responseMessageHandler) {
		super();
		this.responseMessageHandler = responseMessageHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultResponse msg) throws Exception {
		try {
			DefaultResponse response = (DefaultResponse) msg;
			//

			// log.info("DefaultResponse收到消息-" + GsonUtils.toJsonString(msg));

			// BigInteger id =
			// BenCodeUtils.id(response.r().getMap().get(KeyWord.ID).getBytes());
			responseMessageHandler.handler(response);

		} catch (Exception e) {
			try {
				// ctx.writeAndFlush(MessageFactory.createErr(DefaultErro.E_203, "id to long",
				// msg.addr()));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

	}

}
