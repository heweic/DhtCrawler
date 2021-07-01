package org.my.pro.dhtcrawler.netty;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.handler.RequestMessageHandler;
import org.my.pro.dhtcrawler.handler.ResponseMessageHandler;
import org.my.pro.dhtcrawler.message.DefaultRequest;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.BenCodeUtils;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class DhtSimpleChannelInboundHandler extends SimpleChannelInboundHandler<KrpcMessage> {

	private HashMap<String, RequestMessageHandler> map;
	private ResponseMessageHandler responseMessageHandler;

	private Executor executor = Executors.newFixedThreadPool(6);

	public DhtSimpleChannelInboundHandler(HashMap<String, RequestMessageHandler> map,
			ResponseMessageHandler responseMessageHandler) {
		super();
		this.map = map;
		this.responseMessageHandler = responseMessageHandler;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		super.exceptionCaught(ctx, cause);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, KrpcMessage msg) throws Exception {

		executor.execute(new Runnable() {

			@Override
			public void run() {

				KrpcMessage krpcMessage = null;
				//
				if (msg instanceof DefaultRequest) {
					try {
						DefaultRequest request = (DefaultRequest) msg;
						BigInteger id = BenCodeUtils.id(request.a().getMap().get(KeyWord.ID).getBytes());
						// 执行处理

						RequestMessageHandler handler = map.get(request.q());
						if (null == handler) {
//							/System.out.println("unkown command :" + request.q());
						} else {
							krpcMessage = handler.handler(id, request);
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

				} else if (msg instanceof DefaultResponse) {

					try {
						DefaultResponse response = (DefaultResponse) msg;
						BigInteger id = BenCodeUtils.id(response.r().getMap().get(KeyWord.ID).getBytes());
						responseMessageHandler.handler(id, response);

					} catch (Exception e) {
						try {
							// ctx.writeAndFlush(MessageFactory.createErr(DefaultErro.E_203, "id to long",
							// msg.addr()));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}

				}

				//
				if (null != krpcMessage) {
					ctx.writeAndFlush(krpcMessage);
				}

			}
		});

	}

}
