package org.my.pro.dhtcrawler.handler;

import org.my.pro.dhtcrawler.KeyWord;
import org.my.pro.dhtcrawler.KrpcMessage;
import org.my.pro.dhtcrawler.LocalDHTNode;
import org.my.pro.dhtcrawler.RoutingTable;
import org.my.pro.dhtcrawler.message.DefaultResponse;
import org.my.pro.dhtcrawler.util.BenCodeUtils;

/**
 * 检查节点是否存活
 * 
 * 最基础的请求就是ping。这时KPRC协议中的“q”=“ping”。Ping请求包含一个参数id，
 * 它是一个20字节的字符串包含了发送者网络字节序的nodeID。对应的ping回复也包含一个参数id，包含了回复者的nodeID。
 */
public class PingHandler extends RequestMessageHandler {

	public PingHandler(RoutingTable routingTable, LocalDHTNode dhtNode) {
		super(routingTable, dhtNode);
	}

	@Override
	public KrpcMessage handler0(KrpcMessage message) throws Exception {
		//
		DefaultResponse defaultResponse = new DefaultResponse(message.t(), message.addr());

		defaultResponse.setR(BenCodeUtils.to(KeyWord.ID, localNode.id()));
		//
		return defaultResponse;
	}

}
