package org.my.pro.dhtcrawler.netty;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.message.DefaultErro;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.MessageFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理请求 handler
 */
public class DHTRequestChannelInboundHandler extends SimpleChannelInboundHandler<DefaultRequest> {

	public static Log log = LogFactory.getLog(DHTRequestChannelInboundHandler.class);

	/**
	 * 
	 */
	private HashMap<String, RequestMessageHandler> map;

	public DHTRequestChannelInboundHandler(HashMap<String, RequestMessageHandler> map) {
		super();
		this.map = map;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultRequest request) throws Exception {
		//
		KrpcMessage krpcMessage = null;
		try {

			// 执行处理
			RequestMessageHandler handler = map.get(request.q());

			if (null != handler) {
				krpcMessage = handler.handler(request);
			}

		} catch (Exception e) {
			try {
				ctx.writeAndFlush(MessageFactory.createErr(DefaultErro.E_203, "id to long", request.addr(), ""));
			} catch (Exception e1) {
				//
			}
		}
		//
		if (null != krpcMessage) {
			ctx.writeAndFlush(krpcMessage);
		}

	}

}
