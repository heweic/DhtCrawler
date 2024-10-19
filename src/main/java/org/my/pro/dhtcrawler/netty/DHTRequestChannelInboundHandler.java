package org.my.pro.dhtcrawler.netty;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.message.DefaultRequest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理请求 handler
 */
public class DHTRequestChannelInboundHandler extends SimpleChannelInboundHandler<DefaultRequest> {

	private static Log log = LogFactory.getLog(DHTRequestChannelInboundHandler.class);

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
//		KrpcMessage krpcMessage1 = MessageFactory.createFindNode("router.utorrent.com", 6881, localID,
//				NodeIdRandom.generatePeerId());
//		KrpcMessage krpcMessage2 = MessageFactory.createFindNode("dht.transmissionbt.com", 6881, localID,
//				NodeIdRandom.generatePeerId());
//		KrpcMessage krpcMessage3 = MessageFactory.createFindNode("router.bittorrent.com", 6881, localID,
//				NodeIdRandom.generatePeerId());
//
////		ctx.channel().writeAndFlush(krpcMessage1);
////		ctx.channel().writeAndFlush(krpcMessage2);
////		ctx.channel().writeAndFlush(krpcMessage3);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DefaultRequest request) throws Exception {
		KrpcMessage krpcMessage = null;

		try {

			// 执行处理
			// log.info("defaultRequest收到消息:"+ GsonUtils.toJsonString(request));
			RequestMessageHandler handler = map.get(request.q());
			if (null == handler) {
				log.error("未支持的命令:" + request.q());
			} else {
				// log.info("处理节点请求消息:" + request.q());
				krpcMessage = handler.handler(request);
			}

		} catch (Exception e) {
			try {
				// ctx.writeAndFlush(MessageFactory.createErr(DefaultErro.E_203, "id to long",
				// msg.addr()
				// ,""));
			} catch (Exception e1) {

				e1.printStackTrace();
			}
		}
		//
		if (null != krpcMessage) {
			ctx.writeAndFlush(krpcMessage);
		}

	}

}
